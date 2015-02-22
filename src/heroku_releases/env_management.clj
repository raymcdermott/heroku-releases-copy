(ns heroku-releases.env-management
  (:require [clojure.set :as s]
            [heroku-releases.heroku :refer :all]
            [environ.core :refer [env]]))

(def src (or (env :heroku-release-source-app)
             (throw (Exception. (str "You must set HEROKU_RELEASE_SOURCE_APP")))))

(def targets (or (env :heroku-release-target-apps)
                 (throw (Exception. (str "You must set HEROKU_RELEASE_TARGET_APPS")))))

(defn get-env-vars [app-name]
  (let [env-vars (get-heroku-data (str "/apps/" app-name "/config-vars"))]
    (hash-map :app app-name :env env-vars)))

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

; TODO - find a nice way to filter away common differences
; dissoc does not work on sets ;-)

; TODO - same thing for app configuration (not just their env)


;-- Enable lein run -m <namespace>
(defn -main [] (env-variance src targets))

