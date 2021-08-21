(ns clojure-learning.parser
  (:require clojure.string)
  (:use clojure-learning.utils))

(def rules-map
  {
   "for-block"                 '(["for" "symbol" "in" "symbol" "\\{" "statements" "\\}"])
   "statements"          '(["statement" "statements"]
                           ["statement"])
   "statement"           '(["declaration" ";"])
   "declaration"         '(["type" "symbol" "=" "expression"]
                           ["let" "type" "symbol" "be" "expression"])
   "type"                '(["int"] ["double"] ["boolean"] ["symbol"])
   "expression"          '(["expression-literal" "operator" "expression"]
                           ["\\(" "expression" "\\)"]
                           ["expression-literal"])
   "expression-literal"  '(["symbol"] ["number"])

   "boolean-expression"  '(["is" "boolean-expression" "?"]
                           ["expression" "boolean-operator" "expression"]
                           ["symbol"])

   "operator"            '(["math-operator"] ["boolean-operator"])
   "math-operator"       '(["\\+"] ["-"] ["\\*"] ["/"])
   "comparison-operator" '([">"] ["<"] ["\\!" "="] ["=" "="] [])
   "boolean-operator"    '(["and"] ["or"] ["not"])

   "string"              '(["\"" "string-literals" "\""])
   "string-literals"     '([".+" "string-literals"] [".+"])

   "literal"             '(["number"] ["symbol"])
   "number"              '(["double-literal"] ["integer-literal"])
   "double-literal"      '(["\\d*\\.\\d*"])
   "integer-literal"     '(["\\d*"])
   "symbol"              '(["[a-zA-z]+"])})

(defn create-node [name parse-result children]
  {:rule-name name :parse-result parse-result :children children}
  )

(defn add-children [node children]
  (create-node
    (:name node) (:parse-result node) (into (:children node) children))
  )

;todo: needs to know rules tried so far, otherwise recurs infinitely
(defn create-parse-result [matched remaining]
  {:matched matched :remaining remaining}
  )

(defn merge-parse-result [old new]
  {:matched   (into (:matched old) (:matched new))
   :remaining (:remaining new)}
  )

(defn match-to-pattern [tokens pattern]
  (let [token (first tokens)]
    (if (matches? token (re-pattern pattern))
      (create-parse-result [token] (rest tokens))
      (create-parse-result [] tokens)
      )
    )
  )

(declare match-rule)

(defn match-rule-token [previous-state rule-token]
  (let [tokens (:remaining previous-state)]
    (if (nil? (get rules-map rule-token))
      (let [parse-result (match-to-pattern tokens rule-token)]
        (if (empty? (:matched parse-result))
          (create-parse-result [] tokens)
          (merge-parse-result previous-state parse-result)
          )
        )
      (merge-parse-result previous-state (match-rule tokens rule-token))
      )
    )
  )

(defn match-rule-definition [tokens rule-definition]
  (let [no-match {:matched [] :remaining tokens}]
    (if (< (count tokens) (count rule-definition))
      no-match
      (let [match-result (reduce #(match-rule-token %1 %2) no-match rule-definition)]
        (do
          (println (str "matching definition: " rule-definition))
          (if (empty? (:matched match-result))
            no-match
            match-result))
        ))
    )
  )

(defn match-rule [tokens rule-name]
  (let [rule-definitions (get rules-map rule-name)
        no-match {:matched [] :remaining tokens}]
    (if (empty? tokens)
      no-match
      (if rule-definitions
        (reduce #(if (empty? (:matched %1))
                   (match-rule-definition (:remaining %1) %2)
                   (reduced %1))
                no-match rule-definitions)
        (match-to-pattern tokens rule-name)
        ))
    ))

(defn parse [file]
  (-> (slurp file)
      (clojure.string/replace #"[ \t\n\r]+" " ")
      )
  )
