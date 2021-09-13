(ns clojure-rdp.parser.parse-tree)

(defn create-parse-result [matched remaining]
  {:matched matched :remaining remaining}
  )

(defn merge-parse-result [old new]
  {:matched   (into (:matched old) (:matched new))
   :remaining (:remaining new)}
  )

(defn create-node [name parse-result children]
  {:rule-name name :parse-result parse-result :children children}
  )

(defn create-leaf-node [name parse-result] (create-node name parse-result []))

(defn add-child [node child-node]
  (create-node (:rule-name node)
               (merge-parse-result (:parse-result node) (:parse-result child-node))
               (conj (:children node) child-node))
  )

(defn print-node [node n]
  (println
    (str (apply str (repeat n "  "))
         (str "+-" (:rule-name node))
         (if (empty? (:children node))
           (str ": " (:matched (:parse-result node))))
         ))
  (doseq [child (:children node)] (print-node child (inc n)))
  )

(defn visit-tree [root visit-function]
  (doseq [child (:children root)] (visit-function child))
  )

(defn find-in-tree [root rule-name]
  (let [is-rule #(= (:rule-name %1) rule-name)]
    (reduce #(into %1 (find-in-tree %2 rule-name))
            (if (is-rule root) [root] [])
            (:children root)))
  )

(defn find-matched-by-rule
  ([root rule-name]
   (map #(:matched (:parse-result %1))
        (find-in-tree root rule-name)))
  ([root rule-name sub-rule]
   (as-> nil x
         (map #(find-in-tree %1 sub-rule)
              (find-in-tree root rule-name))
         (flatten x)
         (map #(:matched (:parse-result %1)) x)
         ))
  )

(defn matched-by-children [node]
  (map #(str (:rule-name %1) ": " (clojure.string/join " " (:matched (:parse-result %1))))
       (:children node))
  )

(defn find-matched-by-rule2
  ([root rule-name]
   (map matched-by-children
        (find-in-tree root rule-name)))
  ([root rule-name sub-rule]
   (as-> nil x
         (map #(find-in-tree %1 sub-rule)
              (find-in-tree root rule-name))
         (flatten x)
         (map matched-by-children x)
         ))
  )

;filter is lazy, so (first (find-rule..)) will only calculate unntil first match!
(defn find-rule-in-children [root rule-name]
  (filter #(= (:rule-name %1) rule-name) (:children root))
  )

(defn is-pattern? [node]
  (or (clojure.string/includes? (:rule-name node) "*")
      (and (clojure.string/includes? (:rule-name node) "[")
           (clojure.string/includes? (:rule-name node) "]")
           )))

;come nodes (such as parenthesis in function call, commans in arguments etc can be removed!)
(defn collapse [node]
  (let [children (:children node)]
    (if (empty? children)                                   ;or if rule is "argument" or any custom uncollapsible rules?
      node
      (if (= (count children) 1)
        (if (is-pattern? (first children))
          (create-leaf-node (:rule-name node) (:parse-result (collapse (first children))))
          (collapse (first children))
          )
        (create-node (:rule-name node)
                     (:parse-result node)
                     (map #(collapse %1) children))
        ))
    )
  )