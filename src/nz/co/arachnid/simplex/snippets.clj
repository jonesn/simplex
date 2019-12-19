(ns nz.co.arachnid.simplex.snippets
   (:require [nz.co.arachnid.simplex.core :as s])
   (:require [nz.co.arachnid.simplex.tableaux-printer :as t]
             [clojure.walk :as w]))

(def add   (fn [x y] (+ x y)))
(def times (fn [x y] (* x y)))

(def add-5-times-3
  (comp
    (partial times 3)
    (partial add 5)))

(add-5-times-3 10)

(def wee-tree [[1 2 3]
               [4 5 6]])

(w/postwalk #(do (println "visiting:" %) %) wee-tree)
(w/prewalk  #(do (println "visiting:" %) %) wee-tree)

(w/postwalk (fn [x]
              (if (number? x)
                (* 2 x)
                x))
            wee-tree)

(def mid-tree [[1 2 3
                  [5 6 7]]
               [4 5 6
                  [10 13 14]]])

(w/postwalk (fn [x]
              (if (number? x)
                (* 2 x)
                x))
            mid-tree)
