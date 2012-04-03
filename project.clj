(defproject api-proxy "1.0.0-SNAPSHOT"
  :description "Simple API Proxy"
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]]
  :local-repo-classpath true
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib                 "1.2.0"]
                 [swank-clojure/swank-clojure                 "1.4.2"]
                 [ring                                        "1.0.0-RC5"]
                 [clj-http                                    "0.2.5"]
                 [commons-lang                                "2.5"]
                 [org.codehaus.jackson/jackson-core-asl       "1.8.6"]
                 [commons-io/commons-io                       "2.0"]
                 [commons-codec/commons-codec                 "1.4"]
                 [org.clojars.kyleburton/clj-etl-utils        "1.0.44"]])
