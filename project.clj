(defproject yamfood "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :main ^:skip-aot yamfood.core
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "0.2.6"]

                 [kensay/clj-asterisk "0.2.7"]
                 [ring "1.7.1"]
                 [ring/ring-json "0.5.0"]
                 [ring-cors "0.1.13"]
                 [compojure "1.6.1"]
                 [aleph "0.4.6"]
                 [overtone/at-at "1.2.0"]
                 [amazonica "0.3.152"]
                 [environ "1.1.0"]
                 [clj-http "3.10.0"]
                 [morse "0.4.3"]
                 [tongue "0.2.9"]
                 [mount "0.1.16"]
                 [nrepl "0.6.0"]
                 [migratus "1.2.8"]
                 [clj-postgresql "0.7.0"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.3"]
                 [honeysql "0.9.6"]]
  :plugins [[lein-cloverage "1.0.10"]]
  :aot :all
  :uberjar-name "yamfood.jar"
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]
         ;; Initial namespace
         :repl-options   {:init-ns user}
         :jvm-opts     ["-Dio.netty.tryReflectionSetAccessible=true"]
         ;; Additional environment dependent source code
         :source-paths   ["env/dev/clj"]}})
