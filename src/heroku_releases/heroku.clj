(ns heroku-releases.heroku
  (:require [clojure.string :as str]
            [org.httpkit.client :as http]
            [cheshire.core :refer :all]))


(def heroku-api-endpoint "https://api.heroku.com")

(def default-heroku-options {:timeout     30000             ; ms -- 30 seconds
                             :oauth-token (System/getenv "HEROKU_API_TOKEN")
                             :headers     {"Accept: application/vnd.heroku+json; version=3"
                                            "Content-type: application/json"}})

;-- Heroku REST API helper functions
(defn get-heroku-data
  "General function to GET data from the Heroku platform API"
  ([path]
   (get-heroku-data path nil))

  ([path options]
   {:pre [(:oauth-token default-heroku-options)]}
   (let [{:keys [body error]} @(http/get (str heroku-api-endpoint path) (merge options default-heroku-options))]
     (if error (throw (Exception. (str "Failed, exception: " error)))
               (parse-string body true)))))

(defn post-heroku-data
  "General function to POST data to the Heroku platform API"
  ([path json-data]
   (post-heroku-data path json-data nil))

  ([path json-data options]
   {:pre [(:oauth-token default-heroku-options)]}
   (let [options (merge {:form-params json-data} (merge options default-heroku-options))
         {:keys [body error]} @(http/post (str heroku-api-endpoint path) options)]
     (if error (throw (Exception. (str "Failed, exception: " error)))
               (parse-string body true)))))

;-- Exposing parts of the API used by these Use Cases
(defn org-apps [organization]
  "Obtain the list of apps in the organisation"
  (get-heroku-data (str "/organizations/" organization "/apps")))

(defn app-releases [app]
  "Obtain the list of maps that contain release data for the app"
  (get-heroku-data (str "/apps/" app "/releases")))

(defn get-latest-release [app]
  "Obtain the latest release from the (unordered) response"
  (last (sort-by :version < (app-releases app))))

(defn get-specific-release [app version]
  "Obtain a specific release from the (unordered) response"
  (first (filter #(= version (:version %)) (app-releases app))))

(defn copy-release [slug target]
  "Copy the slug to the target app"
  (post-heroku-data (str "/apps/" target "/releases") {"slug" slug}))