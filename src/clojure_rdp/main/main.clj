(ns clojure-rdp.main.main
  (:use clojure-rdp.parser.parser)
  (:use clojure-rdp.parser.parse-tree))

(defn -main []
  (print-tree (parse "hello-word.txt" "program")))