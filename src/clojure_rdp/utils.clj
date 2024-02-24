(ns clojure-rdp.utils)

(defn matches? [string regex]
  (and (not (nil? string))
       ;todo: it's better to pass a regex directly as compiling it every time is expensive
       (re-matches regex string)))