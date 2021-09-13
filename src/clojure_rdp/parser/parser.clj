(ns clojure-rdp.parser.parser
  (:require clojure.string)
  (:use clojure-rdp.utils)
  (:use clojure-rdp.parser.parse-tree)
  (:use clojure-rdp.lexer.lexer))

(def rules-map
  {
   "program"                       '(["blocks"])
   "blocks"                        '(["block" "blocks"] ["block"])
   "block"                         '(["function-block"] ["statement"])
   "block-body"                    '(["\\{" "statements" "\\}"] ["empty-body"])
   "empty-body"                    '(["\\{" "\\}"])

   "function-block"                '(["function-declaration" "block-body"])
   "function-declaration"          '(["function" "symbol" "\\(" "arguments" "\\)"]
                                     ["function" "symbol" "\\(" "\\)"])
   "arguments"                     '(["argument" "," "arguments"] ["argument"])
   "argument"                      '(["variable-declaration"])
   "function-call"                 '(["symbol" "\\(" "parameters" "\\)"])
   "parameters"                    '(["parameter" "," "parameters"] ["parameter"])
   "parameter"                     '(["expression"])

   "for-block"                     '(["for-header" "for-body"])
   "for-header"                    '(["for" "symbol" "in" "expression"])
   "pipeline-block"                '(["pipeline" "symbol" "=" "expression" "block-body"])

   "if-else-block"                 '(["if-block" "else-blocks"] ["if-block"])
   "else-blocks"                   '(["else-if-blocks"] ["else-block"])
   "if-block"                      '(["if" "boolean-expression" "block-body"])
   "else-if-blocks"                '(["else-if-block" "else-if-blocks"] ["else-if-block"])
   "else-if-block"                 '(["else" "if-block"])
   "else-block"                    '(["else" "block-body"])

   "statements"                    '(["statement" "statements"] ["statement"])
   "statement"                     '(["declaration-assignment" ";"]
                                     ["function-call" ";"]
                                     ["for-block"]
                                     ["pipeline-block"]
                                     ["if-else-block"]
                                     ["return" "expression" ";"])
   "declaration-assignment"        '(["variable-declaration" "=" "expression"]
                                     ["let" "variable-declaration" "be" "expression"])
   "variable-declaration"          '(["type" "symbol"])
   "type"                          '(["int"] ["double"] ["boolean"] ["string"]
                                     ["lambda"] ["object"] ["symbol"])
   "expression"                    '(["ternary-expression"] ["math-expression"] ["boolean-expression"] ["string-expression"])
   "string-expression"             '(["string-value" "add-operator" "string-expression"]
                                     ["\\(" "string-expression" "\\)"]
                                     ["string-value"])
   "string-value"                  '(["string-literal"] ["function-call"] ["symbol"])
   "math-expression"               '(["expression-literal" "operator" "math-expression"]
                                     ["\\(" "math-expression" "\\)"]
                                     ["expression-literal"])
   "expression-literal"            '(["function-call"] ["symbol"] ["number"])

   "boolean-expression"            '(["boolean-literal" "boolean-operator" "boolean-expression"]
                                     ["math-expression" "comparison-operator" "math-expression"]
                                     ["\\(" "boolean-expression" "\\)"]
                                     ["not-operator" "boolean-expression"]
                                     ["question"]
                                     ["boolean-literal"]
                                     ["function-call"]
                                     ["symbol"])

   "ternary-expression"            '(["question" "math-expression" ":" "math-expression"]
                                     ["question" "boolean-expression" ":" "boolean-expression"]
                                     ["question" "string-expression" ":" "string-expression"])
   "question"                      '(["is" "boolean-expression" "\\?"])

   "operator"                      '(["math-operator"] ["boolean-operator"])
   "math-operator"                 '(["add-operator"] ["-r"] ["mul-operator"] ["/"])
   "mul-operator"                  '(["\\*"])
   "add-operator"                  '(["\\+"])
   "comparison-operator"           '(["composite-comparison-operator"]
                                     ["basic-comparison-operator"])
   "basic-comparison-operator"     '(["higher" "than"]
                                     ["lower" "than"]
                                     ["equal" "to"]
                                     [">"]
                                     ["<"]
                                     ["\\!" "="]
                                     ["=" "="])
   "composite-comparison-operator" '(["basic-comparison-operator" "boolean-operator" "basic-comparison-operator"])
   "boolean-operator"              '(["and"] ["or"])
   "not-operator"                  '(["not"])

   "string-literal"                '(["\"[^\".]*\""])
   "boolean-literal"               '(["true"] ["false"])
   "literal"                       '(["number"] ["symbol"])
   "number"                        '(["double-literal"] ["integer-literal"])
   "double-literal"                '(["\\d*\\.\\d*"])
   "integer-literal"               '(["\\d*"])
   "symbol"                        '(["[a-zA-z]+"])})

(defn match-to-pattern [tokens pattern]
  (let [token (first tokens)]
    (if (matches? token (re-pattern pattern))
      (create-leaf-node pattern (create-parse-result [token] (vec (rest tokens))))
      )
    )
  )

(declare match-rule)

(defn match-rule-token [tokens rule-token]
  ;(if (and (contains? keywords (first tokens)) (not= rule-token "symbol"))
  ; (throw (Exception. (str "Expecting symbol but found '" (first tokens) "'")))
  (if (get rules-map rule-token)
    (match-rule tokens rule-token)
    (match-to-pattern tokens rule-token)
    )
  ;)
  )

(defn no-match [tokens] {:matched [] :remaining tokens})

(defn match-rule-definition [tokens rule-definition rule-name]
  (if (>= (count tokens) (count rule-definition))
    (reduce #(if-let [result (match-rule-token (:remaining (:parse-result %1)) %2)]
               (add-child %1 result)
               (reduced nil))
            (create-leaf-node rule-name (no-match tokens))
            rule-definition)
    )
  )

(defn match-rule [tokens rule-name]
  ;(println (str rule-name ": " (apply str (take 2 tokens))))
  (if (not-empty tokens)
    (if-let [rule-definitions (get rules-map rule-name)]
      (reduce #(if %1
                 (reduced %1)
                 (match-rule-definition tokens %2 rule-name))
              nil rule-definitions)
      (match-to-pattern tokens rule-name)
      )
    )
  )

(defn parse [file]
  (as-> file x
        (tokenize x)
        (if (nil? x) (throw (Exception. "Could not parse")) x)
        (let [remaining (:remaining (:parse-result x))]
          (if (not-empty remaining)
            (throw (Exception.
                     (str "Error parsing after '"
                          (first remaining) " " (second remaining) "'")))
            x
            ))
        ))

