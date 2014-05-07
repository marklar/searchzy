(ns searchzy.test.service.clean
  (:use midje.sweet)
  (:require [searchzy.service.clean :as clean]))


(let [f (fn [n] [(dec n), nil])
      g (fn [n] [(inc n), nil])
      h (fn [n] [n, :h-error])
      k (fn [n] [n, :k-error])
      ]

  (fact "`bind-short-circuit`"
        (let [bind clean/bind-short-circuit]
          (bind inc [1 nil])      => 2
          (bind inc [1 :i-error]) => [nil :i-error]
          ))

  (fact "`short-circuit->>`"
        (clean/short-circuit->> 1 f g)   => [1 nil]
        (clean/short-circuit->> 1 f g h) => [1 :h-error]
        (clean/short-circuit->> 1 g h)   => [2 :h-error]
        ;; strange, but when there's been a problem, bind sets val to nil.
        (clean/short-circuit->> 1 g h f) => [nil :h-error]
        )

  ;;-------------------------

  (fact "`bind-continue`"
        (let [bind clean/bind-continue]
          (bind f [1 ()])          => [0 ()]
          (bind f [1 '(:i-error)]) => [0 '(:i-error)]
          (bind g [1 ()])          => [2 ()]
          (bind g [1 '(:i-error)]) => [2 '(:i-error)]
          (bind h [1 ()])          => [1 '(:h-error)]
          (bind h [1 '(:i-error)]) => [1 '(:h-error :i-error)]
          ))

  (fact "`gather->>`"
        (clean/gather->> 1 f g)     => [1 ()]
        (clean/gather->> 1 f g h)   => [1 '(:h-error)]
        (clean/gather->> 1 g h)     => [2 '(:h-error)]
        (clean/gather->> 1 g h f)   => [1 '(:h-error)]
        (clean/gather->> 1 g h k f) => [1 '(:k-error :h-error)]
        )

)

