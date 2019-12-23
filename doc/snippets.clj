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

((juxt first second last) (range 10))


;; ============
;; Levenshtein
;; ============

(defn levenshtein* [[c1 & rest1 :as str1]
                    [c2 & rest2 :as str2]]
  (let [len1 (count str1)
        len2 (count str2)]
    (cond (zero? len1) len2
          (zero? len2) len1
          :else
          (min
            (inc (levenshtein* rest1 str2))
            (inc (levenshtein* str1 rest2))
            (+ (if (= c1 c2) 0 1) (levenshtein* rest1 rest2))))))

(def levenshtein (memoize levenshtein*))

(defn to-words [txt init]
  (->> txt
       slurp
       clojure.string/split-lines
       (filter #(.startsWith % init))
       (remove #(> (count %) 8))
       doall))

(defn best [misp dict]
  (->>
    dict
    (map #(-> [% (levenshtein misp %)]))
    (sort-by last)
    (take 3)))

(defn dict [init]
  (to-words "/usr/share/dict/words" init))

(def dict-ac (dict "ac"))

(time (best "achive" dict-ac))

;; =========
;; Lazy Seqs
;; =========

(defn fib
  ([]
   (fib 1 1))
  ([a b]
   (lazy-seq (cons a (fib b (+ a b))))))



