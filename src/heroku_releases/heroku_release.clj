(ns heroku-releases.heroku-release
  (:require [clojure.set :as s]
            [heroku-releases.heroku :as heroku]
            [heroku-releases.mongo-helpers :refer :all]
            [heroku-releases.heroku :as heroku]))

;-- Obtain the latest slug from heroku (deliberately ignoring race conditions)
(defn get-latest-slug [app]
  (let [release (heroku/get-latest-release app)]
    (get-in release [:slug :id])))

(defn record-release [slug]
  ;-- TODO
  )

(defn track-release [app]
  (let [slug (get-latest-slug app)]
    (record-release slug)))



;-- Create a mongo document if not one yet there