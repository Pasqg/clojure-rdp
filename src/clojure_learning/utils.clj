(ns clojure-learning.utils)

(defn matches? [string regex] (and (not (nil? string)) (re-matches regex string)))

(def add
  (fn [x, y]
    (+ x y)
    )
  )

;this is probably bad because cant tell arguments names and types?)
(def add #(+ %1 %2))

;overloaded function has all impls under same name)
(defn overloaded
  ([x] x)
  ([x, y] (+ x y))
  ([x, y, z] (+ x (- y z)))
  )