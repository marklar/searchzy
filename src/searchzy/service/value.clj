(ns searchzy.service.value)

;; (defn cumulative-sums
;;   "[3 2 4 1] => [3 5 9 10]"
;;   [cards]
;;   (reverse
;;    (reduce (fn [res n] (cons (+ n (or (first res) 0)) res))
;;            '() cards)))


;; Price: 40%
;; Yelp Rating: 30%
;; # of Yelp Reviews: 30%
;;
;; Calculation Notes:

;; Rating/Reviews Factor
;;   Rank Rating/Reviews independently by ascending order (i.e., lowest number ranked 1)
;;   Take ranking for Rating/Review and divide by max rank for that bucket
;;   Apply weight to score
;; Price Factor
;;   Rank Price by descending order (i.e., highest number ranked 1)
;;   Take ranking for Price and divide by max rank for that bucket
;;   Apply weight to score
;; Final Score and Sort
;;   Add weighted scores for Ranking, Review, Price
;;   Sort by score and then number of reviews (more reviews, higher)
;;


;; Item:  {:price_micros, :yelp_star_rating, :yelp_review_count}

(defn yelp-tweak
  "Given a biz-menu-item, return a possibly-modified one.
   (If Yelp rating < 3.0, multiply number of reviews by -1.
    If no Yelp reviews, rating=3 and num-reviews=1.)"
  [item]
  (let [r (:yelp_star_rating item)
        c (:yelp_review_count item)]
    (if (= c 0)
      (assoc item :yelp_star_rating 3.0 :yelp_review_count 1)
      (if (< r 3.0)
        (assoc item :yelp_review_count (* -1 c))
        item))))

;;--------

(defn get-ranks
  "[3 2 4 1] => [1 4 6 10]
   Each is the previous plus the new n."
  [cards]
  (reverse
   (drop 1
         (reduce (fn [res n] (cons (+ n (first res)) res))
                 '(1) cards))))

(defn grouped-by-sort-val
  "Given items each with val-attr, return ordered seq of groups."
  [items val-attr]
  (map second (sort (group-by val-attr items))))

(defn add-ranks
  "Add 'rank-attr' attribute to each item, based on its 'val-attr'."
  [items val-attr rank-attr]
  (let [sorted-item-groups (grouped-by-sort-val items)
        cardinalities      (map %(count %) sorted-item-groups)
        ranks              (get-ranks cardinalities)
        rank-2-group       (map vector ranks sorted-item-groups)]
    (flatten
     (map (fn [[r group]] (map #(assoc % rank-attr r) group))
          rank-2-group))))

(defn add-factors-to-ranked-items
  [ranked-items max-rank rank-key factor-key]
  (map #(assoc % factor-key (/ (get % rank-key) max-rank))
       ranked-items))

(defn join-keys
  "Combine multiple keywords into one, joined by '-'."
  [& keywords]
  (keyword (clojure.string/join \- (map name keywords))))

;; Rating/Reviews Factor
;; Rank Rating/Reviews independently by ascending order (i.e., lowest number ranked 1)
;; Take ranking for Rating/Review and divide by max rank for that bucket
;; Apply weight to score
(defn add-factors
  "e.g. [items, :yelp_review_count]
   Adds factor-key (e.g. :yelp_review_count-factor) to each item."
  [items rank-attr]
  (let [rank-key       (join-keys rank-attr :rank)
        factor-key     (join-keys rank-attr :factor)
        factored-items (-> items
                           (add-ranks rank-attr rank-key)
                           (add-factors-to-ranked (count items)
                                                  rank-key factor-key))]
    (map #(dissoc % rank-key) factored-items)))
