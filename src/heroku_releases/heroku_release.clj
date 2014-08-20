(ns heroku-releases.heroku-release
  (:require [clojure.set :as s]
            [heroku-releases.heroku :as heroku]
            [heroku-releases.mongo-helpers :refer :all]
            [heroku-releases.heroku :as heroku]))

;-- Obtain the slug from heroku that matches the requirements
; for example the git commit ID or comment
; TODO: work out how to achieve this!!

(defn get-latest-slug [app]
  (let [release (heroku/get-latest-release app)]
    (get-in release [:slug :id])))

(defn record-release [slug]
  (create-mongo-collection)
  ; TODO: create a configuration for the app
  )

(defn track-release [app]
  (let [slug (get-latest-slug app)]
    (record-release slug)))

;-- Enable lein run -m <namespace>
(defn -main [] (get-latest-slug "bamboo-deploy-test"))
