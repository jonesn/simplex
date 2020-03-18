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

(def wyndor-glass-objective "3x1 + 5x2 = Z")
(def wyndor-glass-constraints [       "x1 <= 4"
                                     "2x2 <= 12"
                               "3x1 + 2x2 <= 18"])


(def max-iteration-pre-wyndor-glass
  {:problem-type              :max
   :iteration                 0
   :basic-variable-row        [:x1 :x2 :s1 :s2 :s3]
   :objective-coeffecient-row [3 5 0 0 0] ;; cj from video
   :tableaux-rows             [{:cbi 0 :active-variable :s1 :constraint-coefficients [1 0 1 0 0] :solution  4 :ratio 0}
                               {:cbi 0 :active-variable :s2 :constraint-coefficients [0 2 0 1 0] :solution 12 :ratio 0}
                               {:cbi 0 :active-variable :s3 :constraint-coefficients [3 2 0 0 1] :solution 18 :ratio 0}]})

(future-fact "Verify that all forms of optimization problems are correctly converted to standard form"
       (fact "Wyndor Glass Simple Max Example can be converted to tableaux form"
             (construct-tableaux-from-objectives-and-constraints wyndor-glass-objective wyndor-glass-constraints) => max-iteration-pre-wyndor-glass))