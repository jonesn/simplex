(ns nz.co.arachnid.simplex.parser-test
  (:require [nz.co.arachnid.simplex.parser :refer :all])
  (:require [midje.sweet                   :refer :all]))

(facts "Check constraint transformations to augmented form."
       (fact "Standard form transform >= to slack and equals."
             (slack-form-of-constraint (constraint-parser "6x1+9x2>=1000") "s1") => (constraint-parser "6x1+9x2+s1=1000")))

(facts "Check we can convert String constraints to Slack / Augmented Form"
       (fact "Vector of constraints in standard form"
             (str-constraints-to-hiccup-augmented-form ["6x1+9x2>=1000" "x1+x2>=100"]) => [[:CONSTRAINT
                                                                                            [:EXPRESSION
                                                                                             [:EXPRESSION [:VARIABLE "6x1"]]
                                                                                             [:OPERATOR "+"]
                                                                                             [:EXPRESSION
                                                                                              [:EXPRESSION [:VARIABLE "9x2"]]
                                                                                              [:OPERATOR "+"]
                                                                                              [:EXPRESSION [:VARIABLE "s1"]]]]
                                                                                            [:COMPARISON "="]
                                                                                            [:RESULT "1000"]]
                                                                                           [:CONSTRAINT
                                                                                             [:EXPRESSION
                                                                                              [:EXPRESSION [:VARIABLE "x1"]]
                                                                                              [:OPERATOR "+"]
                                                                                              [:EXPRESSION
                                                                                               [:EXPRESSION [:VARIABLE "x2"]]
                                                                                               [:OPERATOR "+"]
                                                                                               [:EXPRESSION [:VARIABLE "s2"]]]]
                                                                                             [:COMPARISON "="]
                                                                                             [:RESULT "100"]]]))