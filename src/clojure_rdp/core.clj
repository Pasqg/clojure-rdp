(ns clojure-rdp.core
  (:use clojure-rdp.parser.parser)
  (:use clojure-rdp.parser.utils.rules-parser)
  (:use clojure-rdp.parser.parse-tree)
  (:use clojure-rdp.utils))

(def parsed-tree (parse "hello-word.txt"))

(defn execute
  "I don't do a whole lot."
  [functionName & more]
  (functionName more))