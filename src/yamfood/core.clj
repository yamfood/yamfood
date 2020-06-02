(ns yamfood.core
  (:require
    [aleph.http :as http]
    [aleph.flow :as flow]
    [aleph.netty :as netty]
    [compojure.core :as c]
    [mount.core :as mount]
    [yamfood.api.core :as api]
    [yamfood.web.core :as web]
    [environ.core :refer [env]]
    [compojure.route :as route]
    [yamfood.core.db.init :as db]
    [clojure.tools.logging :as log]
    [yamfood.telegram.core :as telegram]
    [yamfood.core.params.core :as params]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.json :refer [wrap-json-body]]
    [yamfood.tasks.core])                                   ;; order is important
  (:gen-class)
  (:import (io.aleph.dirigiste Stats$Metric Executor)
           (java.util.concurrent TimeUnit)
           (java.util EnumSet)))


(c/defroutes
  app-routes

  (c/context "/" [] web/web-routes)
  (c/context "/api" [] api/api-routes)

  (c/context "/updates" [] telegram/telegram-routes)

  (route/not-found "Not Found"))


(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})))



(mount/defstate ^{:on-reload :noop} executor
  :start (flow/utilization-executor
           0.9 512
           {:metrics (EnumSet/of Stats$Metric/UTILIZATION)})
  :stop (.shutdown executor))



(mount/defstate ^{:on-reload :noop} http-server
  :start
  (let [port (Integer. (or (env :port) 666))]
    (http/start-server #'app {:executor           executor
                              :port               port
                              :join?              false
                              :shutdown-executor? false}))
  :stop
  (do
    (log/info "Waiting for all handlers to return (new requests getting HTTP 503)")
    (.shutdown executor)
    (.awaitTermination executor 10 TimeUnit/SECONDS)
    (.close http-server)
    (netty/wait-for-close http-server)
    (log/info "HTTP Server closed")))


(defn stop-app []
  (log/info "Shutting down gracefully...")
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))


(defn -main []
  (db/init)
  (params/sync-params!)
  (doseq [component (:started (mount/start))]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))


;(def server (-main))
;(.stop server)
