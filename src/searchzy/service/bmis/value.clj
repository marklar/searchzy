(ns searchzy.service.bmis.value
  "Caclulate an 'awesomeness' score for each item and sort by same.

   Suraj says:
   Maybe you can help me on the specifics, but I think we should
   weight it heavier on # yelp reviews and stars and less on price &
   distance. Nothing too drastic, but enough so it won't look like a
   duplicate of our best value."
  (:require [searchzy.service.util :as util]))

;; Item:  {:price_micros, :yelp_star_rating, :yelp_review_count}

(defn- yelp-tweak
  "Given a biz-menu-item, return one with additional attrs
   :tweaked-count and :tweaked-rating.
   (If Yelp rating < 3.0, multiply number of reviews by -1.
    If no Yelp reviews, rating=3 and num-reviews=1.)"
  [item]
  (let [add (fn [item rating count]
              (assoc item :tweaked-rating rating :tweaked-count count))
        rating (-> item :business :yelp_star_rating)
        count  (-> item :business :yelp_review_count)]
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

(defn- rank-norm-fn
  "Create a function which takes:
     * an item-map
     * that item's rank based on 'attr'
   And returns an augmented item-map w/ a 'normalized rank' for that attr.
   A 'normalized rank': rank / max-rank."
  [items attr]
  (let [max-rank (count items)
        norm-key (util/join-keys attr :norm)]
    (fn [item rank]
      (assoc item norm-key (/ rank max-rank)))))

(defn- group-items-by
  [items attr cmp]
  (map second (sort-by first cmp (group-by attr items))))

(defn- add-norm
  "Sort items by attr using cmp (so that better is higher)."
  [items attr cmp]
  (let [;; seq of seqs of items, grouped by attr.
        grouped-items (group-items-by items attr cmp)
        ;; fn : [item rank] -> item w/ 'normed rank'
        norm-rank     (rank-norm-fn items attr)
        ;; norm-rank each item
        [normed-item-groups _]
        (reduce (fn [[res rank] item-group]
                  (let [new-items (map #(norm-rank % rank) item-group)
                        next-rank (+ rank (count item-group))]
                    [(cons new-items res) next-rank]))
                [() 1]
                grouped-items)]
    (flatten normed-item-groups)))

(defn- add-yelp-norms
  "Add :tweaked-count-norm and :tweaked-rating-norm to items.
   Each normed value is between 0 and 1.
   The closer to 1, the better.

   PM's description...
   Rating/Reviews Factor
     Rank Rating/Reviews independently by ascending order (i.e., lowest number ranked 1)
     Take ranking for Rating/Review and divide by max rank for that bucket
     Apply weight to score"
  [items]
  (-> (map yelp-tweak items)
      (add-norm :tweaked-count <)
      (add-norm :tweaked-rating <)))

(defn- add-price-rank-norm
  "PM's description...
   Price Factor
     Rank Price by descending order (i.e., highest number ranked 1)
     Take ranking for Price and divide by max rank for that bucket
     Apply weight to score"
  [items]
  (-> (map price-tweak items)
      (add-norm :tweaked-price >)))

;; -- Currently unused. --
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

;;-----

(defn- compute-score
  [item factors]
  (let [weighted-scores (map (fn [[attr factor]]
                               (* factor (get item attr)))
                             factors)]
    (apply + weighted-scores)))

(defn- add-score
  [factors items]
  (map
   #(assoc % :awesomeness (compute-score % factors))
   items))

;;-----

(defn- rm-norms
  [items]
  (map #(dissoc %
                :tweaked-price
                :tweaked-price-norm
                :tweaked-rating
                :tweaked-rating-norm
                :tweaked-count
                :tweaked-count-norm) items))

(defn- score-and-sort
  [items factors]
  (->> items
       add-yelp-norms
       add-price-rank-norm
       (add-score factors)
       (sort-by (juxt :awesomeness :tweaked-count-norm))
       rm-norms))

;;---------------------------------

(def value-factors
  {:tweaked-price-norm  0.5
   :tweaked-rating-norm 0.25
   :tweaked-count-norm  0.25})

(def rating-factors
  {:tweaked-rating-norm 0.4
   :tweaked-count-norm  0.4
   :tweaked-price-norm  0.1})

(defn value-and-sort
  "Final Score and Sort
   Add weighted scores for Ranking, Review, Price
   Sort (ASC) by score and then number of reviews (more reviews, higher)"
  [items]
  (score-and-sort items value-factors))

(defn rate-and-sort
  "Weigh Yelp more than price, distance."
  [items]
  (score-and-sort items rating-factors))
