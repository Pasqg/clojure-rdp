(ns clojure-rdp.lexer.lexer)

(defn tokenize [file standalone-tokens whitespace-regex]
  (as-> file x
        (slurp x)
        (clojure.string/split x #"\"")
        (let [mapped (as-> x z
                           (take-nth 2 z)
                           (map #(as-> %1 y
                                       (clojure.string/replace y standalone-tokens " $1 ")
                                       (clojure.string/replace y whitespace-regex " ")
                                       (clojure.string/split y #" ")
                                       (filter (fn [s] (not= s "")) y))
                                z))]
          (concat
            (interleave
              mapped
              (map #(str "\"" %1 "\"") (take-nth 2 (rest x))))
            (last mapped))
          )
        (flatten x)
        (vec x)
        ))