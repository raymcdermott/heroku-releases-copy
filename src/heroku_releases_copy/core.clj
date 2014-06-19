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
(assert (not-empty (System/getenv "MONGO_COLLECTION")))


;-- Global constants
(def parameters {:app        (System/getenv "HEROKU_RELEASE_SOURCE_APP")
                 :org        (System/getenv "HEROKU_RELEASE_TARGET_ORG")
                 :targets    (System/getenv "HEROKU_RELEASE_TARGET_APPS")
                 :version    (System/getenv "HEROKU_RELEASE_SOURCE_APP_VERSION")
                 :mongo      (System/getenv "MONGO_URL")
                 :collection (System/getenv "MONGO_COLLECTION")})

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
(defn save-to-mongo [configuration-data]
  (let [uri (:mongo parameters)
        {:keys [conn db]} (mg/connect-via-uri uri)]
    (mc/update db (:collection parameters) configuration-data)
    (mg/disconnect conn)))


(defn get-configuration-from-mongo [app-name]
  (let [uri (:mongo parameters)
        {:keys [conn db]} (mg/connect-via-uri uri)
        configuration-data (mc/find-one-as-map db (:collection parameters) {"app.name" app-name})]
    (mg/disconnect conn)
    (if (nil? configuration-data)
      (throw (Exception. (str "Failed, cannot find configuration for : " app-name)))
      configuration-data)))


;-- Do the work functions
(defn check-regexes [string regexes-as-strings]
  "Check whether one of a list of regexes matches the string"
  (remove nil? (map #(re-matches (re-pattern %) string) regexes-as-strings)))

(defn find-target-apps [organization regexes]
  "Find a filtered list of apps in the organisation whose name matches at least one of the regexes"
  (let [apps (get-heroku-data (str "/organizations/" organization "/apps"))]
    (mapcat #(check-regexes (:name %) regexes) apps)))

(defn app-releases [app]
  (get-heroku-data (str "/apps/" app "/releases") heroku-options))

; TODO -- how to unify these two functions?

(defn get-latest-release [app]
  (let [release (last (sort-by :version < (app-releases app)))]
    (get-in release [:slug :id])))

(defn get-specific-release [app version]
  (let [release (filter #(= version (:version %)) (app-releases app))]
    (get-in release [:slug :id])))

(defn copy-release [slug target]
  (post-heroku-data (str "/apps/" target "/releases") {"slug" slug}))

(defn run-copy []
  (let [configuration-data (get-configuration-from-mongo (:app parameters))
        targets (find-target-apps (:org parameters) (str/split (:targets parameters) #"\s+"))
        release (if (nil? (:version parameters))
                  (get-latest-release (:app parameters))
                  (get-specific-release (:app parameters) (:version parameters)))
        merge-data (partial merge configuration-data)
        heroku-copier (comp save-to-mongo merge-data copy-release)] ; AOP on the cheap!
    (dorun (map #(heroku-copier release %) targets))))

;-- Enable lein run
(defn -main [] (run-copy))




