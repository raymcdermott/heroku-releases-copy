(ns heroku-releases.env-management
  (:require [clojure.set :as s]
            [heroku-releases.heroku :refer :all]
            [heroku-releases.mongo-helpers :refer :all]))

(defn get-env-vars [app-name]
  (let [env-vars (get-heroku-data (str "/apps/" app-name "/config-vars"))]
    (hash-map :app app-name :env env-vars)))

(defn app-differ [app1 app2]
  (let [diff (s/difference (set app1) (set app2))]
    (hash-map :apps (str (:name app1) " vs " (:name app2)) :diff diff )))

(defn env-differ [app1 app2]
  (let [diff (s/difference (set (:env app1)) (set (:env app2)))]
    (hash-map :apps (str (:app app1) " vs " (:app app2)) :diff diff )))

(defn env-variance [app-to-compare & other-apps]
  (let [from-app (get-env-vars app-to-compare)
        comparative-apps (map #(get-env-vars %) other-apps)
        diff (map #(env-differ % from-app) comparative-apps)]
    (println app-to-compare " has this environment configuration: ")
    (println (set (:env from-app)))
    (println diff)))

(defn get-app [app-name]
  (get-heroku-data (str "/apps/" app-name)))

(defn app-differ [app1 app2]
  (let [diff (s/difference (set app1) (set app2))]
    (hash-map :apps (str (:name app1) " vs " (:name app2)) :diff diff )))

(defn app-variance [app-to-compare & other-apps]
  (let [from-app (get-app app-to-compare)
        comparative-apps (map #(get-app %) other-apps)
        diff (map #(app-differ % from-app) comparative-apps)]
    (println app-to-compare " has this application configuration: ")
    (println from-app)
    (println diff)))

; TODO - find a nice way to filter away common differences
; dissoc does not work on sets ;-)

; TODO - write a main program to be executed from the command line

