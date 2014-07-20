(ns lehs.db
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:import org.bson.types.ObjectId))

(def conn (mg/connect))
(def db (mg/get-db conn "mydb"))

(defn get-bb [] (mc/find-maps db "bb"))
(defn bb-entry-to-table-row [r] [:tr {:id (r :_id)} [:td (r :Name)] [:td (r :Content)]])

(defn add-bb-entry [entry] (if (every? entry [:Name :Content])
                             (let [oid (ObjectId.)
                                   doc (select-keys entry [:Name :Content])]
                               (mc/insert db "bb" (merge doc {:_id oid}))
                               oid)))
