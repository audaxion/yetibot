(ns yetibot.observers.score
  (:require
    [taoensso.timbre :refer [info]]
    [yetibot.core.chat :refer [chat-data-structure]]
    [yetibot.core.hooks :refer [obs-hook]]
    [yetibot.models.score :as model]
    [clojure.string :as str]))

(def inc-responses ["+1!" "gained a level!" "is on the rise!" "leveled up!"])
(def dec-responses ["took a hit! Ouch." "took a dive." "lost a life." "lost a level." "pwned."])

(defn- format-thing
  [thing]
  (str/replace thing #"(@\w+)" "<$1>"))

(defn- cmp-user-ids
  [a b]
  (let [[a b] (map #(str/replace-first % #"^@" "") [a b])]
    (= a b)))

(defn score-observer
  [event-info]
  (info "score obs" event-info)
  (let [voter-id (-> event-info :user :id)
        body (:body event-info)]
    (if-let [matches (re-seq #"(\S+)(\+\+|--)" body)]
      (doseq [[_ thing operation] matches]
        (if (cmp-user-ids thing voter-id)
          (chat-data-structure "You can't change your own score!")
          (let [score-delta (condp = operation
                              "++" 1
                              "--" -1)
                response (condp = operation
                           "++" (rand-nth inc-responses)
                           "--" (rand-nth dec-responses))]
            (model/add-score-delta! thing (str "@" voter-id) score-delta)
            (chat-data-structure (str (format-thing thing) " " response " (score: " (model/get-score thing) ")"))))))))

(obs-hook #{:message} #'score-observer)