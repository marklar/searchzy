(ns searchzy.service.bmis.value
  "Caclulate an 'awesomeness' score for each item and sort by same.

   Suraj says:
   Maybe you can help me on the specifics, but I think we should
   weight it heavier on # yelp reviews and stars and less on price &
   distance. Nothing too drastic, but enough so it won't look like a
   duplicate of our best value."
  )

(defn- join-keys
  "Combine multiple keywords into one, joined by '-'."
  [& keywords]
  (keyword (clojure.string/join \- (map name keywords))))

;; Item:  {:price_micros, :yelp_star_rating, :yelp_review_count}

(defn- yelp-tweak
  "Given a biz-menu-item, return one with additional attrs
   :tweaked-count and :tweaked-rating.
   (If Yelp rating < 3.0, multiply number of reviews by -1.
    If no Yelp reviews, rating=3 and num-reviews=1.)"
  [item]
  (let [add (fn [item rating count]
              (assoc item :tweaked-rating rating :tweaked-count count))
        rating (:yelp_star_rating  item)
        count  (:yelp_review_count item)]
    (if (or (nil? count) (= count 0))
      (add item 3.0 1)
      (if (or (nil? rating) (< rating 3.0))
        (add item (or rating 0.0) (* -1 count))
        (add item (or rating 0.0) count)))))

;;
;; If we wish to change the way we compute the norm for prices,
;; we can't use Integer/MAX_VALUE.  But what to use instead?
;;
(defn- price-tweak
  "Ensure that price isn't nil.  Add :tweaked-price."
  [item]
  (assoc item :tweaked-price (or (:price_micros item)
                                 Integer/MAX_VALUE)))
        
;;--------

(defn- add-norm
  "attr: #{:tweaked-count :tweaked-rating :price_micros}
   cmp: affects sort.  < (asc), > (desc)
   Adds norm (e.g. :tweaked-count-norm) to each item."
  [items attr cmp]
  (let [norm-key  (join-keys attr :norm)
        max-rank  (count items)
        normalize (fn [m r] (assoc m norm-key (/ r max-rank)))
        [_ _ _ normed]
        (reduce (fn [[prev-rank prev-val idx res] item]
                  (let [val  (get item attr)
                        rank (if (= val prev-val) prev-rank (inc idx))]
                    [rank val (inc idx) (cons (normalize item rank) res)]))
                [0 nil 0 ()]
                (sort-by attr cmp items))]
    (reverse normed)))

;; Rating/Reviews Factor
;; Rank Rating/Reviews independently by ascending order (i.e., lowest number ranked 1)
;; Take ranking for Rating/Review and divide by max rank for that bucket
;; Apply weight to score
(defn- add-yelp-norms
  [items]
  (-> (map yelp-tweak items)
      (add-norm :tweaked-count <)
      (add-norm :tweaked-rating <)))

;; Price Factor
;;   Rank Price by descending order (i.e., highest number ranked 1)
;;   Take ranking for Price and divide by max rank for that bucket
;;   Apply weight to score
(defn- add-price-rank-norm
  [items]
  (-> (map price-tweak items)
      (add-norm :tweaked-price >)))

(defn- add-price-val-norm
  "Create price norm based not on rank but on actual price.
   If the item's price is nil, use (* 1.1 max-price)."
  [items]
  (let [get-p     #(:price_micros %)
        max-price (apply max (remove nil? (map get-p items)))
        nil-price (* 1.1 max-price)]
    (map #(let [p (or (get-p %) nil-price)]
            (assoc % :tweaked-price-norm (- 1.0 (/ p max-price))))
         items)))

(defn- add-score-to-item
  [item]
  (let [score (+ (* 0.4 (:tweaked-price-norm  item))
                 (* 0.3 (:tweaked-rating-norm item))
                 (* 0.3 (:tweaked-count-norm  item)))]
    (assoc item :awesomeness score)))

(defn- add-score
  [items]
  (map add-score-to-item items))

(defn- score-and-count-lt
  "Comparator for sorting, in ASC order."
  [i1 i2]
  (let [a1 (:awesomeness i1)
        a2 (:awesomeness i2)
        c1 (:tweaked-count-norm i1)
        c2 (:tweaked-count-norm i2)]
    (or (< a1 a2)
        (and (= a1 a2) (< c1 c2)))))

(defn- rm-norms
  [items]
  (map #(dissoc %
                :tweaked-price
                :tweaked-price-norm
                :tweaked-rating
                :tweaked-rating-norm
                :tweaked-count
                :tweaked-count-norm) items))

;;-------------------------------
  
;; Final Score and Sort
;;   Add weighted scores for Ranking, Review, Price
;;   Sort (ASC) by score and then number of reviews (more reviews, higher)
(defn score-and-sort
  [items]
  (let [scored-items (-> items
                         add-yelp-norms
                         ;; add-price-val-norm
                         add-price-rank-norm
                         add-score)]
    (rm-norms (sort score-and-count-lt scored-items))))
