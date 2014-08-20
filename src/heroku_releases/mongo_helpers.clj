(ns heroku-releases.mongo-helpers
  (:require [clojure.string :as str]
            [monger.core :as mg]
            [monger.collection :as mc]))

(defn mongo-action
  "Run a data function on MongoDB wrapped by a connect / disconnect"
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

;-- Admin function -- cannot be generalised into the above form
(defn create-mongo-collection []
  (let [db-uri (System/getenv "MONGO_URL")
        {:keys [conn db]} (mg/connect-via-uri db-uri)
        collection (System/getenv "MONGO_COLLECTION")]
    (if-not (mc/exists? collection)
      (mc/create db collection))
    (mg/disconnect conn)))

(defn save-configuration-to-mongo [configuration-data]
  "Save the release configuration data to MongoDB"
  (mongo-action mc/upsert configuration-data))

(defn get-configuration-from-mongo [app-name]
  "Obtain the release configuration data from MongoDB for app-name"
  (let [configuration-data (mongo-action mc/find-one-as-map {"app.name" app-name})]
    (if (nil? configuration-data)
      (throw (Exception. (str "Failed, cannot find configuration for : " app-name)))
      configuration-data)))

(defn production-indexes []
  (mongo-action mc/ensure-index (array-map :pid.pubId 1))
  (mongo-action mc/ensure-index (array-map :pid.pubId 1 :pid.path 1)))

(defn get-production-indexes
  ([]
   (get-production-indexes (System/getenv "MONGO_URL")))

  ([db-uri]
   {:pre [(string? db-uri)]}
   (get-production-indexes db-uri (System/getenv "MONGO_COLLECTION")))

  ([db-uri collection]
   {:pre [(string? db-uri) (string? collection)]}
   (let [{:keys [conn db]} (mg/connect-via-uri db-uri)
         result (mc/indexes-on db collection)]
     (mg/disconnect conn)
     result)))
