(ns yetibot.commands.score
  (:require
    [yetibot.core.hooks :refer [cmd-hook obs-hook]]
    [yetibot.models.score :as model]
    [clojure.string :as str]))

(defn- format-thing
  [thing]
  (str/replace thing #"(@\w+)" "<$1>"))

(defn high-scores
  "score (--high) (<n>) # show (top n) high scores"
  [{[_ num] :match}]
  (let [top-n (if num
                (Integer/parseInt num)
                10)
        top (model/get-high-scores top-n)]
    (->> (cons "High Scores:"
               (map (fn [score] (str "  " (:score score) " " (format-thing (:thing score)))) top))
         (clojure.string/join "\n"))))

(defn high-givers
  "score (--highgivers) (<n>) # show (top n) high scores"
  [{[_ num] :match}]
  (let [top-n (if num
                (Integer/parseInt num)
                10)
        top (model/get-high-givers top-n)]
    (->> (cons "High Givers:"
               (map (fn [score] (str "  " (:score score) " " (format-thing (:voter-id score)))) top))
         (clojure.string/join "\n"))))

(defn low-scores
  "score --low (<n>) # show (bottom n) low scores"
  [{[_ num] :match}]
  (let [bottom-n (if num
                   (Integer/parseInt num)
                   10)
        bottom (model/get-low-scores bottom-n)]
    (->> (cons "Low Scores:"
               (map (fn [score] (str "  " (:score score) " " (format-thing (:thing score)))) bottom))
         (clojure.string/join "\n"))))

(defn get-score
  "score <thing> # show score for <thing>"
  [{[_ thing] :match}]
  (if-let [score (model/get-score thing)]
    (str (format-thing (:thing score)) " has " (:score score) " points!")
    (str (clojure.string/lower-case thing) " has no points!")))

(cmd-hook #"score"
          #"^$" high-scores
          #"^--highgivers\s*(\d+)?" high-givers
          #"^--high\s*(\d+)?" high-scores
          #"^--low\s*(\d+)?" low-scores
          #"(\w+)" get-score)