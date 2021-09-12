(ns clojure-learning.core
  (:use clojure-learning.parser)
  (:use clojure-learning.rules-parser)
  (:use clojure-learning.parse-tree)
  (:use clojure-learning.transpiler)
  (:use clojure-learning.utils))

(def parsed-tree (parse "hello-word.txt"))

(defn execute
  "I don't do a whole lot."
  [functionName & more]
  (functionName more))