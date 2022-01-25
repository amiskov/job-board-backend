(ns job-board-server.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [job-board-server.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[job-board-server started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[job-board-server has shut down successfully]=-"))
   :middleware wrap-dev})
