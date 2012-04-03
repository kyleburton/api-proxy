(ns api-proxy.core
  (:require
   [clj-etl-utils.log :as log]
   [clojure.contrib.json :as json]
   clojure.contrib.pprint)
  (:use
   ring.adapter.jetty))

;; TODO: serve up proper mime types
;; TODO: forward to remote if no local file found (use clj-http)
;; TODO: load the configuration from config.json
;; TODO: allow multiple proxies to be configured
;; TODO: document how to configure logging, provide a default
;;
;; DONE: serve local content [if found]
;; DONE: serve index.html file if found
;; DONE: have Jetty start in a daemon thread
;; DONE: [re]start jetty server w/simple handler
;; DONE: skeleton project

(defn config-file-path []
  "config.json")

(defn new-config []
  (merge
   {:port 7654
    :jetty (atom nil)
    :join? false
    :document-root "./"
    :stats {:num-requests (java.util.concurrent.atomic.AtomicLong.)}}
   (json/read-json (slurp (config-file-path)))))

(defonce *server* (atom (new-config)))

(defn config [& ks]
  (get-in @*server* ks))

(defn reset-config! []
  (let [jetty-server @(config :jetty)]
    (reset! *server* (new-config))
    (reset! (config :jetty) jetty-server)))


(defn document-root []
  (config :document-root))

(defn server-atom []
  (config :jetty))

(defn count! [& ks]
  (if-not (apply config :stats ks)
    (do
     (reset! *server*
             (update-in
              (config)
              (cons :stats (take (dec (count ks)) ks))
              assoc
              (take-last 1 ks)
              (java.util.concurrent.atomic.AtomicLong.)))
     0)
    (.incrementAndGet (apply config :stats ks))))

;; (reset-config!)
;; (config)
;; (config)
;; (config :stats)
;; (cons :stuff (take (dec (count [:foo :bar])) [:foo :bar]))
;; (count! :stuff)
;; (count! :lots :of :stuff)
;; (take 2 [:lots :of :stuff])
;; (take-last 1 [:lots :of :stuff])

(update-in
 {}
 [:lots :of]
 assoc
 :stuff
 (java.util.concurrent.atomic.AtomicLong.))

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
  (log/infof "Serving local file: %s" f)
  (count! :num-local-files)
  (count! :local-files f)
  {:stutus 200
   :headers {"Content-Type" "text/html"}
   :body (java.io.FileInputStream. f)})

(defn proxy-stats [request]
  {:stutus 200
   :headers {"Content-Type" "text/html"}
   :body    (with-out-str (clojure.contrib.pprint/pprint (config :stats)))})

(defn proxy-request [request]
  {:stutus 404
   :headers {"Content-Type" "text/html"}
   :body    "Implement proxying to the remote server / service"})

(defn request-handler [request]
  (count! :num-requests)
  (def *rr* request)
  (let [local-file-path (find-local-file-path request)]
    (cond
      local-file-path               (serve-local-file request local-file-path)
      (= "/proxy" (:uri request))   (proxy-stats request)
      :else                         (proxy-request request))))


(defn restart-server []
  (reset-config!)
  (when-not (nil? @(server-atom))
    (.stop @(server-atom)))
  (reset! (server-atom) (run-jetty (fn [request] (request-handler request)) (config))))

(restart-server)

(comment

  (def *s* (run-jetty (fn [request] (request-handler request)) *server*))

  *s*



  )



































