(ns heroku-releases.copy-slugs
  (:require [clojure.string :as str]
            [heroku-releases.heroku :as heroku]
            [environ.core :refer [env]]))

;-- Pre-requisites
(def app (or (env :heroku-release-source-app)
             (throw (Exception. (str "You must set HEROKU_RELEASE_SOURCE_APP")))))

(def org (or (env :heroku-release-target-org)
             (throw (Exception. (str "You must set HEROKU_RELEASE_TARGET_ORG")))))

(def targets (or (env :heroku-release-target-apps)
                 (throw (Exception. (str "You must set HEROKU_RELEASE_TARGET_APPS")))))

;-- Helper Functions
(defn check-regexes [string regexes-as-strings]
  "Check whether one of a list of regexes matches the string"
  (remove nil? (map #(re-matches (re-pattern %) string) regexes-as-strings)))

(defn find-target-apps [organization regexes]
  "Find a filtered list of apps in the organisation whose name matches at least one of the regexes"
  (let [apps (heroku/org-apps organization)]
    (mapcat #(check-regexes (:name %) regexes) apps)))

(defn get-slug [app]
  (if-let [release (if (env :heroku-release-source-app-version)
                     (heroku/get-specific-release app (env :heroku-release-source-app-version))
                     (heroku/get-latest-release app))]
    (get-in release [:slug :id])))

;-- Copier program
(defn run-copy []
  "Runs the slug copy function from the source app to each of the target apps"
  (let [slug (get-slug app)
        target-list (find-target-apps org (str/split targets #"\s+"))]
    (dorun (map #(heroku/copy-release slug %) target-list))))

;-- Enable lein run -m <namespace>
(defn -main [] (run-copy))