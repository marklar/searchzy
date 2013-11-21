(ns searchzy.service.value
  "Caclulate an 'awesomeness' score for each item and sort by same."
  )

(defn join-keys
  "Combine multiple keywords into one, joined by '-'."
  [& keywords]
  (keyword (clojure.string/join \- (map name keywords))))

;; Item:  {:price_micros, :yelp_star_rating, :yelp_review_count}

(defn yelp-tweak
  "Given a biz-menu-item, return one with additional attrs
   :tweaked-count and :tweaked-rating.
   (If Yelp rating < 3.0, multiply number of reviews by -1.
    If no Yelp reviews, rating=3 and num-reviews=1.)"
  [item]
  (let [add (fn [i r c] (assoc i :tweaked-rating r :tweaked-count c))
        r (:yelp_star_rating  (:_source item))
        c (:yelp_review_count (:_source item))]
    (if (or (nil? c) (= c 0))
      (add item 3.0 1)
      (if (or (nil? r) (< r 3.0))
        (add item (or r 0.0) (* -1 c))
        (add item (or r 0.0) c)))))

(defn price-tweak
  "Ensure that price isn't null.  Add :tweaked-price."
  [item]
  (assoc item :tweaked-price
    (or (:price_micros item) Integer/MAX_VALUE)))
        
;;--------

(defn add-norm
  "val-attr: #{:tweaked-count :tweaked-rating :price_micros}
   cmp: affects sort.  < (asc), > (desc)
   Adds norm (e.g. :tweaked-count-norm) to each item."
  [maps val-attr cmp]
  (let [norm-key (join-keys val-attr :norm)
        max-rank (count maps)
        add-norm (fn [m r] (assoc m norm-key (/ r max-rank)))
        [_ _ _ normed]
        (reduce (fn [[prev-r prev-v next-r res] m]
                  (let [val (get m val-attr)]
                    (if (= val prev-v)
                      [prev-r val (inc next-r)
                       (cons (add-norm m prev-r) res)]
                      [next-r val (inc next-r)
                       (cons (add-norm m next-r) res)])))
                [0 nil 1 ()]
                (sort-by val-attr cmp maps))]
    (reverse normed)))

;; Rating/Reviews Factor
;; Rank Rating/Reviews independently by ascending order (i.e., lowest number ranked 1)
;; Take ranking for Rating/Review and divide by max rank for that bucket
;; Apply weight to score
(defn add-yelp-norms
  [items]
  (-> (map yelp-tweak items)
      (add-norm :tweaked-count <)
      (add-norm :tweaked-rating <)))

;; Price Factor
;;   Rank Price by descending order (i.e., highest number ranked 1)
;;   Take ranking for Price and divide by max rank for that bucket
;;   Apply weight to score
(defn add-price-norm
  [items]
  (-> (map price-tweak items)
      (add-norm :tweaked-price >)))

(defn add-score-to-item
  [item]
  (let [score (+ (* 0.4 (:tweaked-price-norm  item))
                 (* 0.3 (:tweaked-rating-norm item))
                 (* 0.3 (:tweaked-count-norm  item)))]
    (assoc item :awesomeness score)))

(defn add-score
  [items]
  (map add-score-to-item items))

(defn score-and-count-gt
  "comparator for sorting"
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
