(ns clojure-rdp.lexer.lexer)

(defn tokenize [file]
  (as-> file x
        (slurp x)
        (clojure.string/split x #"\"")
        (let [mapped (as-> x z
                           (take-nth 2 z)
                           (map #(as-> %1 y
                                       (clojure.string/replace y #"([\(\)\{\}\[\]\+\-\*/=;,><\?\!\"])" " $1 ")
                                       (clojure.string/replace y #"[ \t\n\r]+" " ")
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