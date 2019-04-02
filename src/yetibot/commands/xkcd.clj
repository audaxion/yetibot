(ns yetibot.commands.xkcd
  (:require
    [yetibot.core.util.http :refer [get-json]]
    [yetibot.core.hooks :refer [cmd-hook]]))

(defn endpoint
  ([] (str "http://xkcd.com/info.0.json"))
  ([num] (str "http://xkcd.com/" num "/info.0.json")))

(defn xkcd-current-cmd
  "xkcd # fetch current xkcd comic"
  {:yb/cat #{:fun :img}}
  [_]
  ((juxt :title :img :alt) (get-json (endpoint))))

(defn xkcd-cmd
  "xkcd <num> # fetch specific xkcd comic"
  [{[_ num] :match}]
  ((juxt :title :img :alt) (get-json (endpoint num))))

(cmd-hook #"xkcd"
          #"^$" xkcd-current-cmd
          #"^(\d+)" xkcd-cmd
          _ xkcd-current-cmd)
