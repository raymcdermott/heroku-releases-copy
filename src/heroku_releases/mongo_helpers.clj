(ns heroku-releases.mongo-helpers
  (:require [clojure.string :as str]
            [monger.core :as mg]
            [monger.collection :as mc]))


;-- Run a function wrapped by a connect / disconnect
(defn mongo-action
  ([db-fn data]
   (mongo-action (System/getenv "MONGO_URL") db-fn data))

  ([db-uri db-fn data]
   {:pre [(string? db-uri)]}
   (mongo-action db-uri (System/getenv "MONGO_COLLECTION") db-fn data))

  ([db-uri collection db-fn data]
   {:pre [(string? db-uri) (string? collection)]}
   (let [{:keys [conn db]} (mg/connect-via-uri db-uri)
         result (db-fn db collection data)]
     (mg/disconnect conn)
     result)))

;-- Functions to integrate with MongoDB
(defn save-configuration-to-mongo [configuration-data]
  (mongo-action mc/update configuration-data))

(defn get-configuration-from-mongo [app-name]
  (let [configuration-data (mongo-action mc/find-one-as-map {"app.name" app-name})]
    (if (nil? configuration-data)
      (throw (Exception. (str "Failed, cannot find configuration for : " app-name)))
      configuration-data)))