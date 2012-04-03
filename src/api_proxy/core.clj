(ns api-proxy.core
  (:require
   [clj-etl-utils.log :as log]
   clojure.contrib.pprint)
  (:use
   ring.adapter.jetty))

;; TODO: serve local content [if found]
;; TODO: serve index.html file if found
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

(defn document-root []
  "./")

(defn server-atom []
  (get *server* :jetty))

(defn count-request! []
  (.incrementAndGet (get-in *server* [:stats :num-requests])))

(defn find-local-file-path [request]
  (let [f (java.io.File. (document-root)
                         (:uri request))]
    (if (and
         (.isFile f)
         (.exists f))
      (str f)
      ;; try to find /index.html
        (let [f (java.io.File. (document-root)
                               (str (:uri request) "/index.html"))]
    (if (and
         (.isFile f)
         (.exists f))
      (str f))))))


(defn serve-local-file [request f]
  {:stutus 200
   :headers {"Content-Type" "text/html"}
   :body (slurp f)})

(defn proxy-stats [request]
  {:stutus 200
   :headers {"Content-Type" "text/html"}
   :body    (with-out-str (clojure.contrib.pprint/pprint (get *server* :stats)))})

(defn proxy-request [request]
  {:stutus 404
   :headers {"Content-Type" "text/html"}
   :body    "Implement proxying to the remote server / service"})

(defn request-handler [request]
  (count-request!)
  (def *rr* request)
  (let [local-file-path (find-local-file-path request)]
    (cond
      local-file-path               (serve-local-file request local-file-path)
      (= "/proxy" (:uri request))   (proxy-stats request)
      :else                         (proxy-request request))))


(defn restart-server []
  (when-not (nil? @(server-atom))
    (.stop @(server-atom)))
  (reset! (server-atom) (run-jetty (fn [request] (request-handler request)) *server*)))

(restart-server)

(comment

  (def *s* (run-jetty (fn [request] (request-handler request)) *server*))

  *s*



)



































