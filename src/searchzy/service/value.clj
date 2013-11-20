(ns searchzy.service.value
  "Caclulate an 'awesomeness' score for each item and sort by same."
  )

(defn join-keys
  "Combine multiple keywords into one, joined by '-'."
  [& keywords]
  (keyword (clojure.string/join \- (map name keywords))))

;; Item:  {:price_micros, :yelp_star_rating, :yelp_review_count}

(defn yelp-tweak
  "Given a biz-menu-item, return one with additional 'tweaked-' attrs.
   (If Yelp rating < 3.0, multiply number of reviews by -1.
    If no Yelp reviews, rating=3 and num-reviews=1.)"
  [item]
  (let [r (:yelp_star_rating  (:_source item))
        c (:yelp_review_count (:_source item))
        rate-key  :tweaked-rating
        count-key :tweaked-count]
    (if (or (nil? c) (= c 0))
      (assoc item rate-key 3.0 count-key 1)
      (if (or (nil? r) (< r 3.0))
        (assoc item rate-key (or r 0.0) count-key (* -1 c))
        (assoc item rate-key (or r 0.0) count-key c)))))

;;--------

(defn get-ranks
  "[3 2 4 1] => [1 4 6 10]
   Each is the previous plus the new n."
  [cardinalities]
  (reverse
   (drop 1
         (reduce (fn [res n] (cons (+ n (first res)) res))
                 '(1) cardinalities))))

(defn grouped-by-sort-val
  "Given items each with val-attr, return ordered seq of groups."
  [items val-attr cmp]
  (map second (sort cmp (group-by val-attr items))))

(defn add-rank
  "Add 'rank-attr' attribute to each item, based on its 'val-attr'.
   'cmp' affects sort (i.e. either '<' [asc] or '>' [desc])."
  [items val-attr cmp rank-attr]
  (let [sorted-item-groups (grouped-by-sort-val items val-attr cmp)
        cardinalities      (map #(count %) sorted-item-groups)
        ranks              (get-ranks cardinalities)
        rank-2-group       (map vector ranks sorted-item-groups)]
    (flatten
     (map (fn [[r group]] (map #(assoc % rank-attr r) group))
          rank-2-group))))

(defn add-norm-to-ranked-items
  [ranked-items max-rank rank-key norm-key]
  (map #(assoc % norm-key (/ (get % rank-key) max-rank))
       ranked-items))

(defn remove-rank
  [items rank-key]
  (map #(dissoc % rank-key) items))

(defn add-norm
  "rank-attr: #{:tweaked-count :tweaked-rating :price_micros}
   cmp: affects sort.  < (asc), > (desc)
   Adds normed score (e.g. :tweaked-count-norm) to each item."
  [items cmp rank-attr]
  (let [rank-key     (join-keys rank-attr :rank)
        norm-key     (join-keys rank-attr :norm)
        normed-items (-> items
                         (add-rank rank-attr cmp rank-key)
                         (add-norm-to-ranked-items (count items)
                                                   rank-key norm-key))]
    (remove-rank normed-items rank-key)))

(defn first-gt [[a _] [b _]] (> a b))
(defn first-lt [[a _] [b _]] (< a b))

;; Rating/Reviews Factor
;; Rank Rating/Reviews independently by ascending order (i.e., lowest number ranked 1)
;; Take ranking for Rating/Review and divide by max rank for that bucket
;; Apply weight to score
(defn add-yelp-norms
  [items]
  (-> (map yelp-tweak items)
      (add-norm first-lt :tweaked-count)
      (add-norm first-lt :tweaked-rating)))
        
;; Price Factor
;;   Rank Price by descending order (i.e., highest number ranked 1)
;;   Take ranking for Price and divide by max rank for that bucket
;;   Apply weight to score
(defn add-price-norm
  [items]
  (-> items
      (add-norm first-gt :price_micros)))

(defn add-score-to-item
  [item]
  (let [score (+
               (* 0.4 (:price_micros-norm   item))
               (* 0.3 (:tweaked-rating-norm item))
               (* 0.3 (:tweaked-count-norm  item)))]
    (assoc item :awesomeness score)))

(defn add-score
  [items]
  (map add-score-to-item items))

(defn score-and-count-gt
  [i1 i2]
  (let [a1 (:awesomeness i1)
        a2 (:awesomeness i2)
        c1 (:tweaked-count-norm i1)
        c2 (:tweaked-count-norm i2)]
    (or (> a1 a2)
        (and (= a1 a2) (> c1 c2)))))
  
;; Final Score and Sort
;;   Add weighted scores for Ranking, Review, Price
;;   Sort by score and then number of reviews (more reviews, higher)
(defn score-and-sort
  [items]
  (let [scored-items (-> items
                         add-yelp-norms
                         add-price-norm
                         add-score)]
    (sort score-and-count-gt scored-items)))
