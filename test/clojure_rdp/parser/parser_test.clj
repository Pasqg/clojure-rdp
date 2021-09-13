(ns clojure-rdp.parser.parser-test
  (:require [clojure.test :refer :all])
  (:use clojure-rdp.parser.parser))

(is (= (match-to-pattern ["whatever"] "[a-z]+")
       {:rule-name    "[a-z]+"
        :parse-result {:matched ["whatever"] :remaining []}
        :children     []}))
(is (nil? (match-to-pattern ["whatever"] "[0-9]+")))
(is (= (match-rule-definition ["whatever"] ["[a-z]+"] "rule")
       {:rule-name    "rule"
        :parse-result {:matched ["whatever"] :remaining []}
        :children     [{:rule-name    "[a-z]+"
                        :parse-result {:matched ["whatever"] :remaining []}
                        :children     []}]}))
(is (= (match-rule-definition
         ["whatever" "9"]
         ["[a-z]+" "[0-9]"]
         "rule")
       {:rule-name    "rule"
        :parse-result {:matched ["whatever" "9"], :remaining []},
        :children     [{:rule-name    "[a-z]+",
                        :parse-result {:matched ["whatever"], :remaining ["9"]},
                        :children     []}
                       {:rule-name    "[0-9]",
                        :parse-result {:matched ["9"], :remaining []},
                        :children     []}]}))
(is (nil? (match-rule-definition ["whatever"] ["[0-9]+"] "rule")))
(is (nil? (match-rule-definition ["whatever"] ["[a-z]+" "[a-z]+"] "rule")))