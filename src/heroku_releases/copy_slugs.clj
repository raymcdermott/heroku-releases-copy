(ns heroku-releases.copy-slugs
  (:require [clojure.string :as str]
            [heroku-releases.heroku :as heroku]
            [heroku-releases.mongo-helpers :refer :all]))

;-- Helper Functions
(defn check-regexes [string regexes-as-strings]
  "Check whether one of a list of regexes matches the string"
  (remove nil? (map #(re-matches (re-pattern %) string) regexes-as-strings)))

(defn find-target-apps [organization regexes]
  "Find a filtered list of apps in the organisation whose name matches at least one of the regexes"
  (let [apps (heroku/org-apps organization)]
    (mapcat #(check-regexes (:name %) regexes) apps)))

(defn get-latest-slug [app]
  (let [release (heroku/get-latest-release app)]
    (get-in release [:slug :id])))

(defn get-specific-slug [app version]
  (let [release (heroku/get-specific-release app version)]
    (get-in release [:slug :id])))

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
   (let [configuration-data (get-configuration-from-mongo app)
         version (System/getenv "HEROKU_RELEASE_SOURCE_APP_VERSION")
         slug (if (nil? version)
                (get-latest-slug app)
                (get-specific-slug app version))
         target-list (find-target-apps org (str/split targets #"\s+"))
         merge-data (partial merge configuration-data)
         heroku-copier (comp save-configuration-to-mongo merge-data heroku/copy-release)] ; AOP on the cheap!
     (println (str " targets " (count target-list) " slug " slug "cd " configuration-data))
     (dorun (map #(heroku-copier slug %) target-list)))))

;-- Enable lein run -m <namespace>
(defn -main [] (run-copy))