(ns clojure-learning.rules-parser
  (:require clojure.string)
  (:use clojure-learning.utils))

(defn parse-rule [rule-string]
  (let [results (map #(clojure.string/split % #" ")
                     (clojure.string/split rule-string #" :rule-def: "))] ;consider adding :or: to distinguishi between multiple definitions
    (vector (first (first results)) (rest results))
    )
  )

(defn parse-rules [file-name]
  (-> (slurp file-name)
      (clojure.string/replace #"[ \t\n\r]+" " ")
      (clojure.string/split #" :end: ")
      (#(->> %
             (map parse-rule)
             (into {})))
      )
  )

(defn parse-tuple-until-token
  [tokens-vector parsed-tokens stop-token-regex]
  (let [first-element (first tokens-vector)
        second-element (second tokens-vector)
        remaining (rest (rest tokens-vector))]
    (if (matches? first-element stop-token-regex)
      [parsed-tokens tokens-vector]
      (if (matches? second-element stop-token-regex)
        (throw (Exception. (str "Not expecting '" second-element
                                "' after '" first-element
                                "' (stop-token-regex: '" stop-token-regex "')")))
        (let [result (parse-tuple-until-token remaining parsed-tokens stop-token-regex)]
          [(conj (first result) first-element) (second result)]
          )
        )
      )
    )
  )

(defn parse-next
  [tokens-vector termination-tokens rules]
  (let [current (first tokens-vector)
        remaining (rest tokens-vector)]
    (case current
      ".termination-tokens" (let [result (parse-tuple-until-token remaining termination-tokens #".rules")]
                              (parse-next (second result) (first result) rules)
                              )
      ".rules" (let [result (parse-tuple-until-token remaining rules #".end")]
                 (parse-next (second result) termination-tokens (first result))
                 )
      ".end" [termination-tokens rules]
      )
    )
  )

(defn parse-rules-old [file-name]
  (let [file-content (slurp file-name)
        file-tokens (clojure.string/split file-content #"[ \n\t\r]+")]
    (parse-next file-tokens (list) (list))
    ))