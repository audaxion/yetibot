(ns yetibot.db.score
  (:require
    [yetibot.core.db.util :as db.util]))

(def schema {:schema/table "score"
             :schema/specs (into [[:thing :text "NOT NULL"]
                                  [:points :integer "NOT NULL"]
                                  [:voter-id :text "NOT NULL"]]
                                 (db.util/default-fields))})

(def create (partial db.util/create (:schema/table schema)))

(def delete (partial db.util/delete (:schema/table schema)))

(def find-all (partial db.util/find-all (:schema/table schema)))

(def query (partial db.util/query (:schema/table schema)))
