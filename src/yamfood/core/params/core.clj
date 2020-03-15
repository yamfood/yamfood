(ns yamfood.core.params.core
  (:require
    [clojure.edn :as edn]
    [honeysql.core :as hs]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


(def params-query
  {:select [:params.name
            :params.value]
   :from   [:params]})


(defn params->map
  [params-list]
  (reduce
    #(assoc
       %1
       (keyword (:name %2))
       (edn/read-string (:value %2)))
    {}
    params-list))


(def default-params
  {:disable-card false
   :verify-phone true})


(defn params!
  []
  (->> params-query
       (hs/format)
       (jdbc/query db/db)
       (params->map)
       (merge default-params)))
