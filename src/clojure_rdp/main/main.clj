(ns clojure-rdp.main.main
  (:use clojure-rdp.parser.parser)
  (:use clojure-rdp.parser.parse-tree))

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

(defn -main []
  (print-tree (collapse (parse "hello-word.txt" "program" rules-map))))