(ns api-proxy.core
  (:require
   [clj-etl-utils.log :as log]
   clojure.contrib.pprint)
  (:use
   ring.adapter.jetty))

;; TODO: serve local content [if found]
;; TODO: forward to remote if no local file found (use clj-http)
;; TODO: load the configuration from config.json
;; TODO: allow multiple proxies to be configured
;; TODO: document how to configure logging, provide a default
;;
;; DONE: have Jetty start in a daemon thread
;; DONE: [re]start jetty server w/simple handler
;; DONE: skeleton project

(defonce *server* {:port 7654
               :jetty (atom nil)
               :join? false
               :stats {
                       :num-requests (java.util.concurrent.atomic.AtomicLong.)
                       }})

(defn server-atom []
  (get *server* :jetty))

(defn count-request! []
  (.incrementAndGet (get-in *server* [:stats :num-requests])))

(defn request-handler [request]
  (def *rr* request)
  (log/infof "got a request: %s" request)
  {:stutus 200
   :headers {"Content-Type" "text/html"}
   :body (format "Request %s satisfied at: %s :: <pre>%s</pre>"
                 (count-request!)
                 (java.util.Date.)
                 (with-out-str (clojure.contrib.pprint/pprint request)))})

(defn restart-server []
  (when-not (nil? @(server-atom))
    (.stop @(server-atom)))
  (reset! (server-atom) (run-jetty (fn [request] (request-handler request)) *server*)))

(restart-server)

(comment

  (def *s* (run-jetty (fn [request] (request-handler request)) *server*))

  *s*



)