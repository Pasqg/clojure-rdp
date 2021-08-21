(ns clojure-learning.core
  (:use clojure-learning.parser)
  (:use clojure-learning.rules-parser)
  (:use clojure-learning.utils))

(defn execute
  "I don't do a whole lot."
  [functionName & more]
  (functionName more))