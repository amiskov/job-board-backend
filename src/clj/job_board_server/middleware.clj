(ns job-board-server.middleware
  (:require
    [job-board-server.env :refer [defaults]]
    [clojure.tools.logging :as log]
    [job-board-server.layout :refer [error-page]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [job-board-server.middleware.formats :as formats]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [job-board-server.config :refer [env]]
    [ring.middleware.flash :refer [wrap-flash]]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.adapter.undertow.middleware.session :refer [wrap-session]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))


(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status  500
                     :title   "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title  "Invalid anti-forgery token"})}))

(defn cors-handler [handler]
  (prn handler)
  (wrap-cors
    handler
    :access-control-allow-origin [#"http://localhost:8000" #"https://amiskov.github.io/job-board-frontend"]
    :access-control-allow-methods [:get]))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))
      wrap-internal-error))
