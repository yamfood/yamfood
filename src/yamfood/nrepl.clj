(ns yamfood.nrepl
  (:require
    [mount.core :as mount]
    [nrepl.server :as nrepl]
    [environ.core :refer [env]]
    [clojure.tools.logging :as log])
  (:import (org.apache.commons.io.output WriterOutputStream)
           (java.io PrintStream)))


(defn rebind-output
  "Use this, if you don't see output in remote repl"
  []
  (log/info "Rebinding output...")
  (System/setOut (PrintStream. (WriterOutputStream. *out*) true))
  (System/setErr (PrintStream. (WriterOutputStream. *err*) true))
  (alter-var-root #'*out* (fn [_] *out*))
  (alter-var-root #'*err* (fn [_] *err*)))


(comment (rebind-output))

(mount/defstate ^{:on-reload :noop} server
  :start
  (when (env :nrepl-port)
    (do
      (log/info "Starting REPL server on port" (env :nrepl-bind) ":" (Integer. (env :nrepl-port)))
      (nrepl/start-server :port (Integer. (env :nrepl-port)) :bind (env :nrepl-bind))))
  :stop
  (when (env :nrepl-port)
    (nrepl/stop-server server)))
