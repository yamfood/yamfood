(ns yamfood.core.db.core
  (:require [environ.core :refer [env]]))

(def db {:classname      "org.postgresql.Driver"
         :connection-uri (env :jdbc-database-url)})
