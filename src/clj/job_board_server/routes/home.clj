(ns job-board-server.routes.home
  (:require
    [job-board-server.layout :as layout]
    [clojure.walk :refer [keywordize-keys]]
    [clojure.contrib.humanize :as h]
    [clojure.string :as str]
    [job-board-server.middleware :as middleware]
    [ring.util.response]
    [cheshire.core :as ch]
    [job-board-server.config :refer [env]]
    [mount.core :refer [defstate]]
    [ring.util.http-response :as response])
  (:import [com.algolia.search DefaultSearchClient]
           [com.google.gson Gson]
           [com.algolia.search.models.indexing Query SearchResult]))

(java.security.Security/setProperty "networkaddress.cache.ttl", "60")

(defstate ^{:on-reload :noop} algolia-index
          :start (let [app-id (env :algolia-app-id)
                       api-key (env :algolia-api-key)
                       algolia-client (DefaultSearchClient/create app-id api-key)
                       index (.initIndex algolia-client "jobs")]
                   index))

(defn search-algolia [q]
  (.search algolia-index (Query. q)))                       ;


(defn parse-remun [hit]
  (str (if (get-in hit [:remuneration :competitive])
         "Competitive"
         (let [r (:remuneration hit)
               ->k #(str (/ % 1000) "K")]
           (str (->k (:min r)) " - " (->k (:max r)))))
       (when (get-in hit [:remuneration :equity]) " + Equity")))

(defn smaller-logo-url [logo-url]
  (let [id (last (str/split logo-url #"/"))
        cdn-url "https://workshub.imgix.net/"
        img-params "?fit=clip&crop=entropy&auto=format&w=40&h=40"]
    (str cdn-url id img-params)))

(defn parse-hit [hit]
  {:last-modified-ts    (:last-modified hit)
   :last-modified-human (h/datetime (:last-modified hit))
   :title               (:title hit)
   :company-country     (str (get-in hit [:company :name]) ", " (get-in hit [:location :country]))
   :company-size        (get-in hit [:company :size])
   :logo                (smaller-logo-url (get-in hit [:company :logo]))
   :tags                (map #(select-keys % [:label :objectID]) (:tags hit))
   :role-type           (:role-type hit)
   :tagline             (:tagline hit)
   :remote              (:remote hit)
   :perks               []
   :remuneration        (parse-remun hit)})

(defn parse-hits [raw-hits]
  (let [g (new Gson)
        hits (->> raw-hits
                  ;; TODO: This needs to be improved.
                  (.toJson g)
                  (ch/parse-string)
                  (keywordize-keys))]
    (map parse-hit hits)))

(defn parse-search-result [^SearchResult sr]
  {:pages         (.getNbPages sr)
   :current-page  (.getPage sr)
   :nb-hits       (.getNbHits sr)
   :hits          (parse-hits (.getHits sr))
   :hits-per-page (.getHitsPerPage sr)})

(defn home-page [request]
  (layout/render request "home.html" {:docs "It works!"}))

(defn search [{:keys [params]}]
  (let [q (:query params)
        resp (->> q
                  (search-algolia)
                  (parse-search-result))]
    (response/ok resp)))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-cors
                 middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/search" {:get search}]])

(comment



  (smaller-logo-url "https://functionalworks-backend--prod.s3.amazonaws.com/logos/b99b94fd5cf03260daed09943b18e596")


  ;b99b94fd5cf03260daed09943b18e596
  (parse-search-result (search-algolia "clojure"))
  #_end)
