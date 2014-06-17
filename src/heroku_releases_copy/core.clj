(ns heroku-releases-copy.core
  (:require [clojure.string :as str]
            [org.httpkit.client :as http]
            [cheshire.core :refer :all]
            [monger.core :as mg]
            [monger.collection :as mc]))


;-- Mandatory pre-requisites
(assert (not-empty (System/getenv "HEROKU_API_TOKEN")))
(assert (not-empty (System/getenv "HEROKU_RELEASE_SOURCE_APP")))
(assert (not-empty (System/getenv "HEROKU_RELEASE_TARGET_ORG")))
(assert (not-empty (System/getenv "HEROKU_RELEASE_TARGET_APPS")))
(assert (not-empty (System/getenv "MONGO_URL")))


;-- Global constants
(def parameters {:app     (System/getenv "HEROKU_RELEASE_SOURCE_APP")
                 :org     (System/getenv "HEROKU_RELEASE_TARGET_ORG")
                 :targets (System/getenv "HEROKU_RELEASE_TARGET_APPS")
                 :version (System/getenv "HEROKU_RELEASE_SOURCE_APP_VERSION")})

(def heroku-api-endpoint "https://api.heroku.com")

(def heroku-options {:timeout     30000                     ; ms -- 30 seconds
                     :oauth-token (System/getenv "HEROKU_API_TOKEN")
                     :headers     {"Accept: application/vnd.heroku+json; version=3"
                                    "Content-type: application/json"}})


;-- Heroku REST API helper functions
(defn get-heroku-data
  ([path] (get-heroku-data path nil))
  ([path options]
   (let [{:keys [body error]} @(http/get (str heroku-api-endpoint path) (merge options heroku-options))]
     (if error (throw (Exception. (str "Failed, exception: " error)))
               (parse-string body true)))))

(defn post-heroku-data
  ([path json-data] post-heroku-data path json-data nil)
  ([path json-data options]
   (let [options (merge {:form-params json-data} (merge options heroku-options))
         {:keys [body error]} @(http/post (str heroku-api-endpoint path) options)]
     (if error (throw (Exception. (str "Failed, exception: " error)))
               (parse-string body true)))))


;-- MongoDB helper functions
(defn save-to-mongo [configuration-data deployment-data]
  (let [uri (System/getenv "MONGO_URL")
        {:keys [conn db]} (mg/connect-via-uri uri)]
; TODO -- update the data
    (mc/insert-and-return db "orgAppEnv" configuration-data)
    (mg/disconnect conn)))


(defn get-configuration-from-mongo [app-name]
  (let [uri (System/getenv "MONGO_URL")
        {:keys [conn db]} (mg/connect-via-uri uri)
        configuration-data (mc/find-one db "orgAppEnv" {"app.name" app-name})]
    (mg/disconnect conn)
    configuration-data))



;-- Do the work functions

(defn check-regexes [string regexes-as-strings]
  "Check whether one of a list of regexes matches the string"
  (remove nil? (map #(re-matches (re-pattern %) string) regexes-as-strings)))

(defn find-target-apps [organization regexes]
  "Find a filtered list of apps in the organisation whose name matches the regex for example
    (target-apps 'tme-web-preview' 'bamboo-app1')
    (target-apps 'tme-web-preview' 'bamboo-.*')
    (target-apps 'tme-web-preview' '.*-[a-z]{2}')"
  (let [apps (get-heroku-data (str "/organizations/" organization "/apps"))
        filtered-apps (mapcat #(check-regexes (:name %) regexes) apps)]
    filtered-apps))

(defn app-releases [app]
  "Find releases for app"
  (get-heroku-data (str "/apps/" app "/releases") heroku-options))

(defn get-release
  "Get the release data for the most recent (or specific) version of app"
  ([app] (let [releases (app-releases app)
               sorted-list (sort-by :version < releases)]
           (last sorted-list)))
  ([app version] (let [releases (app-releases app)]
                   (filter #(= version (:version %)) releases))))

(defn copy-release [slug target]
  (post-heroku-data (str "/apps/" target "/releases") {"slug" slug}))

(defn copy-releases [slug targets]
  (doall (map #(copy-release slug %) targets)))

(defn run-copy []
  (let [configuration-data (get-configuration-from-mongo (:app parameters))
        targets (find-target-apps (:org parameters) (str/split (:targets parameters) #"\s+"))
        release (if (nil? (:version parameters))
                  (get-release (:app parameters))
                  (get-release (:app parameters) (:version parameters)))
        deployment-data (copy-releases (get-in release [:slug :id]) targets)]
    (save-to-mongo configuration-data deployment-data)))

;-- Enable lein run
(defn -main [] (run-copy))



