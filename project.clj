(defproject yamfood "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]

                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [compojure "1.6.1"]

                 [environ "1.1.0"]

                 [migratus "1.2.4"]
                 [clj-postgresql "0.7.0"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [honeysql "0.9.6"]]
  :uberjar-name "yamfood.jar"
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
