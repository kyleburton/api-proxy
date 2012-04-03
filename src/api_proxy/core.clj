(ns api-proxy.core
  (:require
   [clj-etl-utils.log :as log])
  (:use
   ring.adapter.jetty))

;; TODO:
;;  * load the configuration from config.json
;;  * allow multiple proxies to be configured
;;  * document how to configure logging, provide a default

(def *server* {:port 7654
               :jetty (atom nil)})

(defn server-atom []
  (get *server* :jetty))

(defn request-handler [request]
  (def *rr* request)
  (log/infof "got a request: %s" request)
  {:stutus 200
   :body "Yes, we got the request."})

(defn restart-server []
  (when-not (nil? @(server-atom))
    (.stop @(server-atom)))
  (reset! (server-atom) (run-jetty request-handler *server*)))


(comment

  (restart-server)

)