(ns nz.co.arachnid.simplex.parser
  (:require [instaparse.core :as insta]))

;; Parser to deal with Linear Constraint setup.
(def constraint-parser
  (insta/parser
    "CONSTRAINT = EXPRESSION <whitespace>* COMPARISON <whitespace>* RESULT
     EXPRESSION = <whitespace>* VARIABLE <whitespace>* | ( EXPRESSION  <whitespace>* OPERATOR <whitespace>* EXPRESSION )
     COMPARISON = '<=' | '=' | '>='
        VARIABLE = 'x'#'[0-9]+'
        OPERATOR = '+'  |  '-'
        RESULT = #'[0-9]+'
        whitespace = #'\\s+'"))

(comment
  (constraint-parser "x1 + x2 = 0")
  (constraint-parser "x1+x2>=100")
  (constraint-parser "x1+x2+x3+x4<=10001"))

