(ns kafka-toy.acceptance
  (:require [kafka-toy.test-common :refer :all]
            [clj-http.client :as http]
            [environ.core :refer [env]]
            [cheshire.core :as json]
            [midje.sweet :refer :all])
  (:import [java.util UUID]))

(defn gen-uid
  []
  (str (UUID/randomUUID)))

(fact-group
 :acceptance

 (fact "Ping resource returns 200 HTTP response"
       (let [response (http/get (url+ "/ping")  {:throw-exceptions false})]
         response => (contains {:status 200})))

 (fact "Healthcheck resource returns 200 HTTP response"
       (let [response (http/get (url+ "/healthcheck") {:throw-exceptions false})
             body (json-body response)]
         response => (contains {:status 200})
         body => (contains {:name "kafka-toy"
                            :success true
                            :version truthy})))

 (fact "create a topic on kafka!!"
       (let [{:keys [status]} (http/put (url+ (str "/topic/" gen-uid)) {:throw-exceptions false})]
         status => 200))

 (fact "list all messages on kafka"
       (let [topic (gen-uid)
             topic-url (str "/topic/" topic)
             _ (http/put (url+ topic-url))
             message {:guid (gen-uid)}
             _ (http/post (url+ topic-url) {:body (json/generate-string message)
                                                :content-type :json
                                                :throw-exceptions false})
             {:keys [status body]} (http/get (url+ topic-url) {:content-type :json
                                                                   :throw-exceptions false
                                                                   :as :json})]
         status => 200
         body => (contains {:messages (contains message)})))

 (fact "return 404 if topic does not exist"
       (http/get (url+ "/topic/does-not-exist-topic") {:throw-exceptions false})
       =>
       (contains {:status 404}))

 (fact "return empty topic if no messages"
       (let [topic (gen-uid)
             _ (http/put (url+ (str "/topic/" topic)))
             {:keys [status body]} (http/get (url+ (str "/topic/" topic))
                                             {:content-type :json
                                              :throw-exceptions false
                                              :as :json})]
         status => 200
         body => (contains {:messages []}))))
