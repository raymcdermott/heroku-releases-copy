(ns heroku-releases.copy-slugs
  (:require [clojure.string :as str]
            [heroku-releases.heroku-helpers :refer :all]
            [heroku-releases.mongo-helpers :refer :all]))


;-- Helper Functions
(defn check-regexes [string regexes-as-strings]
  "Check whether one of a list of regexes matches the string"
  (remove nil? (map #(re-matches (re-pattern %) string) regexes-as-strings)))

(defn find-target-apps [organization regexes]
  "Find a filtered list of apps in the organisation whose name matches at least one of the regexes"
  (let [apps (get-heroku-data (str "/organizations/" organization "/apps"))]
    (mapcat #(check-regexes (:name %) regexes) apps)))

(defn app-releases [app]
  "Obtain the list of maps that contain release data for the app"
  (get-heroku-data (str "/apps/" app "/releases")))

(defn slug-from-app-release [lookup-fn app]
  "Use the lookup-fn to obtain the relevant release and then pluck out the slug id"
  (let [release (last (lookup-fn (app-releases app)))]
    (get-in release [:slug :id])))

(defn get-slug
  "Define and call partial functions to get the slug id"
  ([app] (get-slug app (System/getenv "HEROKU_RELEASE_SOURCE_APP_VERSION")))
  ([app version]
   (if (nil? version)
     (slug-from-app-release (partial sort-by :version <) app)
     (slug-from-app-release (partial filter #(= version (:version %))) app))))

(defn copy-release [slug target]
  (post-heroku-data (str "/apps/" target "/releases") {"slug" slug}))

;-- Combination program
(defn run-copy
  "Runs the slug copy function from the source app to each of the target apps and tracks the results in the database"
  ([]
   (let [app (System/getenv "HEROKU_RELEASE_SOURCE_APP")
         org (System/getenv "HEROKU_RELEASE_TARGET_ORG")
         targets (System/getenv "HEROKU_RELEASE_TARGET_APPS")]
     (run-copy app org targets)))
  ([app org targets]
   {:pre [(string? app) (string? org) (string? targets)]}
   (let [configuration-data (get-configuration-from-mongo (app))
         slug (get-slug app)
         target-list (find-target-apps org (str/split targets #"\s+"))
         merge-data (partial merge configuration-data)
         heroku-copier (comp save-configuration-to-mongo merge-data copy-release)] ; AOP on the cheap!
     (dorun (map #(heroku-copier slug %) target-list)))))

;-- Enable lein run
(defn -main [] (run-copy))

