(ns job-board-server.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[job-board-server started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[job-board-server has shut down successfully]=-"))
   :middleware identity})
