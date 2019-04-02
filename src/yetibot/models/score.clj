(ns yetibot.models.score
  (:require
    [yetibot.db.score :as db]
    [clj-time.coerce :as time.coerce]))

(defn add-score-delta!
  [thing voter-id points]
  (db/create {:thing thing
              :voter-id voter-id
              :points points}))

(defn get-score
  [thing]
  (let [score (-> (db/query {:select/clause "SUM(points) as score"
                             :where/map {:thing thing}})
                  first :score)]
    (if (nil? score) 0 score)))

(defn get-high-scores
  ([] (get-high-scores 10))
  ([cnt]
   (let [cnt (if (or (<= cnt 0) (> cnt 100)) 10 cnt)]
     (db/query {:select/clause "thing, SUM(points) as score"
                :group/clause "thing"
                :having/clause "SUM(points) > 0"
                :order/clause "score DESC"
                :limit/clause cnt}))))

(defn get-low-scores
  ([] (get-low-scores 10))
  ([cnt]
   (let [cnt (if (or (<= cnt 0) (> cnt 100)) 10 cnt)]
     (db/query {:select/clause "thing, SUM(points) as score"
                :group/clause "thing"
                :having/clause "SUM(points) > 0"
                :order/clause "score ASC"
                :limit/clause cnt}))))

(defn get-high-givers
  ([] (get-high-givers 10))
  ([cnt]
   (let [cnt (if (or (<= cnt 0) (> cnt 100)) 10 cnt)]
     (db/query {:select/clause "voter_id, SUM(points) as score"
                :group/clause "voter_id"
                :having/clause "SUM(points) > 0"
                :order/clause "score DESC"
                :limit/clause cnt}))))

(defn delete-thing!
  [thing]
  (doseq [id (map :id (db/query {:select/clause "id"
                                 :where/map {thing thing}}))]
    (db/delete id)))