(ns nz.co.arachnid.simplex.core-test
  (:require [midje.sweet :refer :all])
  (:require [nz.co.arachnid.simplex.core :refer :all]))

;; ======================
;; Max Problem Iterations
;; ======================

(def iteration-0-pre
  {:problem-type              :max
   :iteration                 0
   :basic-variable-row        [:x1 :x2 :s1 :s2]
   :objective-coeffecient-row [12 16 0 0] ;; cj from video
   :tableaux-rows             [{:cbi                     0
                                :active-variable         :s1
                                :constraint-coefficients [10 20 1 0]
                                :solution                120
                                :ratio                   0}
                               {:cbi                     0
                                :active-variable         :s2
                                :constraint-coefficients [8 8 0 1]
                                :solution                80
                                :ratio                   0}]})

(def iteration-0-post
  {:problem-type              :max
   :iteration                 0
   :basic-variable-row        [:x1 :x2 :s1 :s2]
   :objective-coeffecient-row [12 16 0 0]
   :tableaux-rows             [{:cbi 0
                                :active-variable :s1
                                :constraint-coefficients [10 20 1 0],
                                :solution 120,
                                :ratio 6}
                               {:cbi 0
                                :active-variable :s2
                                :constraint-coefficients [8 8 0 1],
                                :solution 80,
                                :ratio 10}]
   :Cj-Zj                     [12 16 0 0]
   :Zj-row                    [0 0 0 0]
   :key-column-index          1
   :key-element               20
   :key-ratio-index           0
   :key-row-index             0
   :entering-variable         :x2
   :exiting-variable          :s1})

(facts "Calculate ZJ Row Cases"
       (fact "Given Iteration 0 we will correctly calculate a zero Zj row"
             (:Zj-row (calculate-zj-row iteration-0-pre)) => [0 0 0 0]))

(facts "Calculate Cj - Zj"
       (fact "Given iteration 0 we will correctly calculate the Cj-Zj row"
             (let [undertest (comp calculate-cj-zj-row calculate-zj-row)]
               (:Cj-Zj (undertest iteration-0-pre)) => [12 16 0 0])))

(facts "Check Optimality"
       (fact "Given iteration 0 we will deduce that the max optimum has not been meet"
             (let [undertest (comp optimal-solution? calculate-cj-zj-row calculate-zj-row)]
               (undertest iteration-0-pre) => false)))

(facts "Calculate Key Column"
       (fact "Given iteration 0 we can correctly select the key column"
             (let [undertest (comp calculate-key-column calculate-cj-zj-row calculate-zj-row)
                   result    (undertest iteration-0-pre)]
               (:key-column-index result) => 1)))

(facts "Calculate Key Column Ratios"
       (fact "Given iteration 0 we can correctly calculate the s / k ratios"
             (let [undertest (comp calculate-solution-to-key-val-ratio
                                   calculate-key-column
                                   calculate-cj-zj-row
                                   calculate-zj-row)
                   result    (undertest iteration-0-pre)]
               (:key-ratio-index result) => 0
               (:tableaux-rows result)   => [{:cbi 0, :active-variable :s1 :constraint-coefficients [10 20 1 0], :solution 120, :ratio  6}
                                             {:cbi 0, :active-variable :s2 :constraint-coefficients [8 8 0 1],   :solution  80, :ratio 10}])))


(facts "Calculate Key Row and Key Value"
       (fact "Given iteration 0 we can correctly calculate the key row and key value"
             (let [undertest (comp calculate-key-row-and-value
                                   calculate-solution-to-key-val-ratio
                                   calculate-key-column
                                   calculate-cj-zj-row
                                   calculate-zj-row)
                   result    (undertest iteration-0-pre)]
               (:key-row-index result) => 0
               (:key-element   result) => 20)))


(facts "Calculate Entering and Exiting Variables"
       (fact "Given iteration 0 with the key rows and columns calculated then we can calculate the exiting value."
             (let [undertest (comp calculate-entering-and-exiting-variables
                                   calculate-key-row-and-value
                                   calculate-solution-to-key-val-ratio
                                   calculate-key-column
                                   calculate-cj-zj-row
                                   calculate-zj-row)
                   result    (undertest iteration-0-pre)]
               (:tableaux-rows result)     => [{:cbi 0, :active-variable :s1 :constraint-coefficients [10 20 1 0], :solution 120, :ratio  6}
                                               {:cbi 0, :active-variable :s2 :constraint-coefficients [8 8 0 1],   :solution  80, :ratio 10}]
               (:exiting-variable result)  => :s1
               (:entering-variable result) => :x2
               result                      => iteration-0-post)))

;; ======================
;; Min Problem Iterations
;; ======================

;; ======================
;;   Helper Function Tests
;; ======================

(facts "Calculate Entering Row should use the key element (ke) to create a ratio of old coeffecient over ke"
       (fact "Given a well formed row the function will correctly calculate the new ratios"
             (let [input       {:cbi 16, :active-variable :x1, :constraint-coefficients [10 20 1 0], :solution 120, :ratio 6}
                   key-element 20]
               (calculate-entering-row input key-element) => {:cbi 16, :active-variable :x1, :constraint-coefficients [1/2 1 1/20 0], :solution 6, :ratio 1})))

(facts "Calculate Non Entering Rows will update all non key rows ready for next iteration."
       (fact "Given a well form vector of rows the function can correctly calculate new rows"
             (let [previous-key-row {:cbi 0, :active-variable :s1 :constraint-coefficients [10 20 1 0], :solution 120, :ratio  6}
                   key-element      20
                   key-row-index    0
                   input            [{:cbi 16, :active-variable :x1, :constraint-coefficients [1/2 1 1/20 0], :solution 6, :ratio 1}
                                     {:cbi 0, :active-variable :s2, :constraint-coefficients [8 8 0 1], :solution 80, :ratio 10}]]
               (calculate-non-entering-rows input previous-key-row key-row-index 0 key-element) => [{:cbi 16, :active-variable :x1, :constraint-coefficients [1/2 1 1/20 0], :solution  6, :ratio 1}
                                                                                                    {:cbi  0, :active-variable :s2, :constraint-coefficients [  4 0 -2/5 1], :solution 32, :ratio 10}])))

;; ======================
;;     Zombie Cases
;; ======================
