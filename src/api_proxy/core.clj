(ns api-proxy.core
  (:require
   [clj-etl-utils.log :as log]
   [clojure.contrib.json :as json]
   [clj-etl-utils.http :as ua]
   clojure.contrib.pprint)
  (:use
   ring.adapter.jetty))

;; TODO: forward to remote if no local file found (use clj-http)
;; TODO: load the configuration from config.json
;; TODO: allow multiple proxies to be configured
;; TODO: document how to configure logging, provide a default
;; TODO: allow mime types file to be specified in configuration
;; TODO: allow default mime type to be specified in the configuration
;; TODO: cookie rewriting - just act as a pass-through
;;
;; DONE: serve up proper mime types
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

(defn mime-types-from-nginx-config []
  (let [mime-type-file (if (.exists (java.io.File. "/etc/nginx/mime.types"))
                         "/etc/nginx/mime.types"
                         "resources/mime.types")
        mime-types (slurp mime-type-file)
        spos        (.indexOf mime-types "{")
        epos        (.indexOf mime-types "}")
        lines       (drop 1 (vec (.split (.substring mime-types spos epos) "\n")))]
    (reduce
     (fn make-mime-types-map [m [mime-type & extensions]]
       (loop [m m
              [ext & exts] extensions]
         (if-not ext
           m
           (recur (assoc m ext mime-type)
                  exts))))
     {}
     (map
      (fn parse-nginx-mime-type-line [l]
        (vec
         (.split
          (.replaceAll (.trim l) ";" "")
          "\\s+")))
      lines))))

(def *mime-types*
     (mime-types-from-nginx-config))

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

(defn mime-type-for-file [f]
  (let [ext       (org.apache.commons.io.FilenameUtils/getExtension f)]
    (get *mime-types* (.toLowerCase ext) "text/html")))

;; (mime-type-for-file "foo.css")

(defn serve-local-file [request f]
  (log/infof "Serving local file: %s" f)
  (count! :num-local-files)
  (count! :local-files f)
  {:stutus 200
   :headers {"Content-Type" (mime-type-for-file f)}
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


  (def *ua* (ua/user-agent))


)

