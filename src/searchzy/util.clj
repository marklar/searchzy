(ns searchzy.util)

(defn doseq-cnt
  "Call function 'f' on each of 'seq', printing count each 'num'."
  [f num seq]
  (let [cnt (atom 0)]
    (doseq [i seq]
      (f i)
      (swap! cnt inc)
      (if (= 0 (mod @cnt num))
        (println @cnt)))
    @cnt))

