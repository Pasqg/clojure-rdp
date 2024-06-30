(ns clojure-rdp.parser.parser
  (:require clojure.string)
  (:use clojure-rdp.utils)
  (:use clojure-rdp.parser.parse-tree)
  (:use clojure-rdp.lexer.lexer))

(defn match-to-pattern [tokens pattern]
  (let [token (first tokens)]
    (if (matches? token (re-pattern pattern))
      (create-leaf-node pattern (create-parse-result [token] (vec (rest tokens))))
      )
    )
  )

(declare match-rule)

(defn match-rule-token [tokens rule-token rules-map]
  (if (get rules-map rule-token)
    (match-rule tokens rule-token rules-map)
    (match-to-pattern tokens rule-token)
    )
  ;)
  )

(defn no-match [tokens] {:matched [] :remaining tokens})

(defn match-rule-definition [tokens rule-definition rule-name rules-map]
  (if (>= (count tokens) (count rule-definition))
    (reduce #(if-let [result (match-rule-token (:remaining (:parse-result %1)) %2 rules-map)]
               (add-child %1 result)
               (reduced nil))
            (create-leaf-node rule-name (no-match tokens))
            rule-definition)
    )
  )

(defn match-rule [tokens rule-name rules-map]
  (if (not-empty tokens)
    (if-let [rule-definitions (get rules-map rule-name)]
      (reduce #(if %1
                 (reduced %1)
                 (match-rule-definition tokens %2 rule-name rules-map))
              nil rule-definitions)
      (match-to-pattern tokens rule-name)
      )
    )
  )

(defn parse
  ([file rule rules-map] (parse file rule rules-map #"([\(\)\{\}\[\]\+\-\*/=;,><\?\!\"])"))
  ([file rule rules-map standalone-tokens] (parse file rule rules-map standalone-tokens #"[ \t\n\r]+"))
  ([file rule rules-map standalone-tokens whitespace-regex]
   (as-> file x
         (tokenize x standalone-tokens whitespace-regex)
         (match-rule x rule rules-map)
         (if (nil? x) (throw (Exception. "Could not parse")) x)
         (let [remaining (:remaining (:parse-result x))]
           (if (not-empty remaining)
             (throw (Exception.
                      (str "Error parsing after '"
                           (first remaining) " " (second remaining) "'")))
             x
             ))
         )))

