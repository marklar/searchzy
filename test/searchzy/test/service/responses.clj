(ns searchzy.test.service.responses
  (:use midje.sweet)
  (:require [searchzy.service.responses :as responses]))

(let [obj {:a [1 2 3]}
      body "{\"a\":[1,2,3]}"]

  (fact "`json-response`"
        (let [f #'responses/json-response]
          (f ..status.. obj) => (contains {:status ..status..
                                           :body body})
          ))
  
  (fact "`json-p-ify`"
        (let [f responses/json-p-ify]
          (f {:status ..status..
              :headers ..headers..
              :body body})
          => {:status ..status..
              :headers ..headers..
              :body (str "jsonCallBack(" body ")")}
          ))

  (fact "`??-json`"
        (responses/ok-json obj)        => (contains {:status 200, :body body})
        (responses/forbidden-json obj) => (contains {:status 403, :body body})
        (responses/error-json obj)     => (contains {:status 404, :body body})
        )

  )
