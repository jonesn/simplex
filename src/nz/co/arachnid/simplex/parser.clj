(ns nz.co.arachnid.simplex.parser
  (:require [instaparse.core :as insta]
            [clojure.zip :as zip]))

;; Parser to deal with Linear Constraint setup.
(def constraint-parser
  (insta/parser
    "CONSTRAINT = EXPRESSION <whitespace>* COMPARISON <whitespace>* RESULT
     EXPRESSION = <whitespace>* VARIABLE <whitespace>* | ( EXPRESSION  <whitespace>* OPERATOR <whitespace>* EXPRESSION )
     COMPARISON = '<=' | '=' | '>='
        VARIABLE = #'[0-9]*[x|s][0-9]+'
        OPERATOR = '+'  |  '-'
        RESULT = #'[0-9]+'
        whitespace = #'\\s+'"))


(defn pred-gen-vec-and-keyword
  "Generates a predicate function for use in spectre operations.
   The resulting function will ensure the given object is a vector
   and that its first element matches the given keyword.
   I.e. (pred-gen-vec-and-keyword :RESULT)
   would match [:RESULT 0]"
  [keyword]
  (fn [element]
    (and (vector? element)
         (= keyword (first element)))))


(defn replace-first-instance-in-hiccup-tree
  "## Description
   Recursively navigates a hiccup vector structure and replaces the *first* node identified by target-node-predicate
   by the node defined by target value.

   ## Parameters
   - target-node-predicate: a predicate that takes one parameter that is the node to identify.
   - target-value: The new value of the node to be replaced.
   - hiccup-content: The vector of hiccup content to parse.

   ## Example Hiccup Content
   ```
   [:CONSTRAINT
     [:EXPRESSION
      [:EXPRESSION [:VARIABLE \"6x1\"]]
      [:OPERATOR \"+\"]
      [:EXPRESSION [:VARIABLE \"9x2\"]]]
     [:COMPARISON \">=\"]
     [:RESULT \"1000\"]]
   ```"
  [target-node-predicate target-value hiccup-content]
  (loop [hiccup-tree (zip/vector-zip hiccup-content)]
    (cond
       ;; Base case is we have traversed the data structure
       (zip/end? hiccup-tree) (zip/root hiccup-tree)
       ;; If our current node is a match for our predicate then replace and exit
       (target-node-predicate (zip/node hiccup-tree)) (zip/root (zip/edit hiccup-tree (constantly target-value)))
       ;; Otherwise Continue Traversal.
       :else                                           (recur (zip/next hiccup-tree)))))


(defn slack-form-of-constraint
  "## Description
   Creates a new hiccup representation of the given constraint in slack form.
   A slack variable represents the amount of slack in the left hand side of an inequality.
   As an example
   ```
   x1 <= 4
   ```
   The slack variable for this constraint is defined to be:
   ```
   x3 = 4 - x1
   ```
   Which is the amount of slack in the left hand of the inequality. Thus,
   ```
   x1 + x3 = 4
   ```

   ## Parameters
   - The Hiccup Vector Representing the constraint. (See below)
   - The slack variable name to be used in this form. I.e. s1, s2, s3

   ## Example Hiccup Content
   ```
   [:CONSTRAINT
     [:EXPRESSION
      [:EXPRESSION [:VARIABLE \"6x1\"]]
      [:OPERATOR \"+\"]
      [:EXPRESSION [:VARIABLE \"9x2\"]]]
     [:COMPARISON \">=\"]
     [:RESULT \"1000\"]]
   ```
   "
  [hiccup-vector-of-constraint slack-variable]
  (let [hiccup-tree-seq     (tree-seq vector? identity hiccup-vector-of-constraint)
        expression          (->> hiccup-tree-seq
                                 (filter (pred-gen-vec-and-keyword :EXPRESSION))
                                 (last))
        comparison          (->> hiccup-tree-seq
                                 (filter (pred-gen-vec-and-keyword :COMPARISON))
                                 (last))
        slack-expression    [:EXPRESSION expression [:OPERATOR "+"] [:EXPRESSION [:VARIABLE slack-variable]]]
        updated-with-slack  (replace-first-instance-in-hiccup-tree
                              (fn [node] (= node expression))
                              slack-expression
                              hiccup-vector-of-constraint)
        updated-with-equals (replace-first-instance-in-hiccup-tree
                              (fn [node] (= node comparison))
                              [:COMPARISON "="]
                              updated-with-slack)]
    updated-with-equals))


(defn instaparse-to-string
  [instaparse-form]
  nil)

(comment
  (constraint-parser "x1 + x2 = 0")
  (constraint-parser "x1 + x2 + s2 = 0")
  (constraint-parser "x1 + x2 + j2 = 0")
  (constraint-parser "x1+x2>=100")
  (clojure.pprint/pprint (constraint-parser "6x1+9x2>=1000"))
  (clojure.pprint/pprint (slack-form-of-constraint (constraint-parser "6x1+9x2>=1000") "s1"))
  (def full-constraint
    [:CONSTRAINT
     [:EXPRESSION
      [:EXPRESSION [:VARIABLE "6x1"]]
      [:OPERATOR "+"]
      [:EXPRESSION [:VARIABLE "9x2"]]]
     [:COMPARISON ">="]
     [:RESULT "1000"]])
  (constraint-parser "x1+x2+x3+x4<=10001")
  (s/select [s/ALL  (pred-gen-vec-and-keyword :EXPRESSION)] nested-exp)
  (clojure.pprint/pprint (s/select [s/ALL (s/walker (pred-gen-vec-and-keyword :EXPRESSION))] nested-exp))
  (clojure.pprint/pprint (s/select [s/ALL (s/codewalker (pred-gen-vec-and-keyword :EXPRESSION))] full-constraint))
  (clojure.pprint/pprint (s/select [s/ALL  (pred-gen-vec-and-keyword :EXPRESSION)] full-constraint))
  (->> (tree-seq vector? identity full-constraint)
       (filter (pred-gen-vec-and-keyword :EXPRESSION))
       (last))
  (replace-first-instance-in-hiccup-tree
    (fn [node] (= node [:EXPRESSION [:VARIABLE "9x2"]]))
    [:poo]
    full-constraint)
  (clojure.pprint/pprint
    (replace-first-instance-in-hiccup-tree
      (fn [node] (= node [:EXPRESSION [:VARIABLE "9x2"]]))
      [:poo [:EXPRESSION [:VARIABLE "9x2"]]]
      full-constraint))
  (apply (fn [node] (= node [:EXPRESSION [:VARIABLE "9x2"]])) [[:EXPRESSION [:VARIABLE "9x2"]]]))



