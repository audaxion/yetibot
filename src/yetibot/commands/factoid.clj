(ns yetibot.commands.factoid
  (:require
    [clojure.string :refer [join split re-quote-replacement]]
    [clojure.set :refer [intersection]]
    [taoensso.timbre :refer [info warn error]]
    [yetibot.core.chat :refer [chat-data-structure]]
    [yetibot.core.hooks :refer [obs-hook cmd-hook]]
    [yetibot.models.factoid :as model]))

(def factoid-history (atom {}))

(def factoid-start "(?i)(\\s|\\b|^)")
(def factoid-end "(\\s|\\b|$)")

(defn add-factoid-for [chat-source factoid-id]
  (swap! factoid-history assoc chat-source factoid-id))

(defn trigger-factoid-response?
  [string]
  (->> (map #(if (re-find (re-pattern (str factoid-start % factoid-end)) string) %)
            (model/get-triggers))
       (distinct)
       (remove #(empty? %))
       (seq)))

(defn listen-for-trigger
  [event-json]
  (when-not (re-find #"^!" (:body event-json))
    (if-let [matches (trigger-factoid-response? (:body event-json))]
      (let [response (-> (rand-nth matches)
                         (model/random-factoid-for))]
        (add-factoid-for (:chat-source event-json) (:id response))
        (chat-data-structure (:response response))))))

(defn last-factoid-for-room
  "factoid last # show the last factoid triggered in the current room"
  {:yb/cat #{:fun}}
  [{:keys [chat-source]}]
  (when @factoid-history
    (if-let [factoid (model/get-factoid (get @factoid-history chat-source))]
      (str "The last factoid triggered here was for trigger: '"
           (:trigger factoid)
           "' with response: '"
           (:response factoid)
           "' (ID: "
           (:id factoid)
           ")")
      (str "No factoid found!"))))

(defn delete-factoid
  "factoid delete: <id> # delete a response for a trigger"
  {:yb/cat #{:fun}}
  [{[_ id] :match}]
  (if-let [_deleted (model/remove-factoid (Long/parseLong id))]
    (str "OK. Deleted " id)
    (str "Factoid does not exist!")))

(defn factoid-search
  "factoid search: <search> # search for a trigger and show responses"
  {:yb/cat #{:fun}}
  [{[_ trigger] :match}]
  (if-let [search (model/fuzzy-get-factoids-for trigger)]
    (if (map? (first search))
      (doall
        (let [factoids (map (fn [factoid]
                              (str (:response factoid) " (ID: " (:id factoid) ")"))
                            search)
              factoids-str (join "\n" factoids)]
          (str "Showing factoids for: '" (:trigger (first search)) "'\n"
               factoids-str)))
      (first search))
    (str "No factoids for '" trigger "'")))

(defn add-factoid
  "factoid add: <trigger> /// <response> # add a factoid"
  {:yb/cat #{:fun}}
  [{[_ trigger response] :match}]
  (if-let [_result (model/add trigger response)]
    (str "OK. Added '" response "' for '" trigger "'")
    (str "Factoid '" response "' for '" trigger "' already exists!")))

(cmd-hook #"factoid"
          #"^add:\s*(.+?)\s*\/\/\/\s*(.*)$" add-factoid
          #"^delete:\s*(\d+)" delete-factoid
          #"^search:\s*(.+)" factoid-search
          #"^last$" last-factoid-for-room)

(obs-hook #{:message}
          #'listen-for-trigger)