(ns yetibot.models.factoid
  (:require
    [yetibot.db.factoid :as db]
    [clojure.string :refer [join split lower-case] :as s]
    [clj-fuzzy.metrics :refer [levenshtein]]
    [taoensso.timbre :refer [info warn error spy]]))

(def distance-threshold "Minimum distance for fuzzy prefix matching" 3)

(def distance-range
  "The range of acceptible distances to look for, beginning with 0 up to
   `distance-threshold` exclusive"
  (range (inc distance-threshold)))

;; db helpers

(defn get-factoids-for [trigger]
  (db/query {:where/map {:trigger trigger}}))

(def triggers (atom nil))

(defn get-triggers []
  @triggers)

(defn reload-triggers []
  (reset! triggers (->> (db/find-all)
                        (map #(:trigger %))
                        (distinct))))

(reload-triggers)

(defn add-factoid [trigger response]
  (db/create {:trigger trigger :response response})
  (reload-triggers))

(defn remove-factoid [id]
  (db/delete id)
  (reload-triggers))

(defn get-factoid [id]
  (-> (db/query {:where/map {:id id}})
      (first)))

(defn distance-to-prefix
  "Take a string and return prefixes grouped by distance"
  [s]
  (let [dist (partial levenshtein s)]
    (->> (get-triggers) (group-by dist))))

(defn nearest-match-within-range
  [s]
  (let [prefix-distances (distance-to-prefix s)]
    (some
      #(when-let [topics (prefix-distances %)] [% topics])
      distance-range)))

(defn fuzzy-get-factoids-for
  "Tries to find docs for a given topic based on Levenshtein distance. If the
   Levenshtein distance is within `distance-range`, it'll either:
   - return help for the nearest distance topic, only if no other topics share
     the same distance
   - return the list of topics with the same distance"
  [topic]
  (when-let [[dist matches] (nearest-match-within-range topic)]
    (if (> (count matches) 1)
      ; multiple matches, suggest them to the user
      [(str "No matches for \"" topic "\". Did you mean: "
            (s/join ", " matches) "?")]
      (get-factoids-for (first matches)))))

(defn random-factoid-for [trigger]
  (rand-nth (get-factoids-for trigger)))

(defn add [trigger response]
  (let [responses (->> (get-factoids-for trigger)
                       (map #(:response %)))]
    (clojure.pprint/pprint responses)
    (when-not (some #(= response %) responses)
      (add-factoid trigger response))))