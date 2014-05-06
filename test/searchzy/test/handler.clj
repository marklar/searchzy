(ns searchzy.test.handler
  (:use clojure.test
        ring.mock.request  
        searchzy.service.handler))

(deftest test-app

  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) 
             "{\"message\":\"Welcome to Searchzy!\",\"params\":{}}"))))
  
  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
