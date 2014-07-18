(ns lehs.db
  (:require [monger.core :as mg]
            [monger.collection :as mc]))

(def conn (mg/connect))
(def db (mg/get-db conn "mydb"))
(defn add-bb-entry [entry] (if (every? entry [:Name :Content])
                             (mc/insert db "bb" (select-keys entry [:Name :Content]))))
(defn get-bb [] (mc/find-maps db "bb"))
(defn bb-entry-to-table-row [r] [:tr [:td (r :Name ":")] [:td (r :Content)]])
