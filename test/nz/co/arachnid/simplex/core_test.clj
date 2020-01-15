(ns nz.co.arachnid.simplex.core-test
  (:require [midje.sweet :refer :all])
  (:require [nz.co.arachnid.simplex.core :refer :all]))

;; ======================
;; Max Problem Iterations
;; ======================

;; Maximize z = 12x1 + 16x2
;; ========================
;;
;; Subject to:
;; ===========
;; - 10x1 + 20x2 <= 120
;; -  8x1 +  8x2 <= 80
;; - x1 and x2 >= 0
;;
;; Slack Variable Form
;; ===================
;; - Max z: 12x1 + 16x2 + 0s1 + 0s2 (Objective Function)
;; - 10x1 + 20x2 + s1 = 120         (Constraint 1)
;; -  8x1 +  8x2 + s2 = 80          (Constraint 2)

(def max-iteration-0-pre
  {:problem-type              :max
   :iteration                 0
   :basic-variable-row        [:x1 :x2 :s1 :s2]
   :objective-coeffecient-row [12 16 0 0] ;; cj from video
   :tableaux-rows             [{:cbi 0 :active-variable :s1 :constraint-coefficients [10 20 1 0] :solution 120 :ratio 0}
                               {:cbi 0 :active-variable :s2 :constraint-coefficients [8 8 0 1]   :solution 80 :ratio 0}]})

(def max-iteration-0-post
  {:problem-type              :max
   :iteration                 0
   :basic-variable-row        [:x1 :x2 :s1 :s2]
   :objective-coeffecient-row [12 16 0 0]
   :tableaux-rows             [{:cbi 0 :active-variable :s1 :constraint-coefficients [10 20 1 0], :solution 120, :ratio 6}
                               {:cbi 0 :active-variable :s2 :constraint-coefficients [8 8 0 1],   :solution 80,  :ratio 10}]
   :cj-zj-row                 [12 16 0 0]
   :zj-row                    [0 0 0 0]
   :key-column-index          1
   :key-element               20
   :key-ratio-index           0
   :key-row-index             0
   :entering-variable         :x2
   :exiting-variable          :s1})

(def max-iteration-1-pre
  {:problem-type              :max
   :iteration                 1
   :basic-variable-row        [:x1 :x2 :s1 :s2]
   :objective-coeffecient-row [12 16 0 0]
   :tableaux-rows             [{:cbi 16, :active-variable :x2, :constraint-coefficients [1/2 1 1/20 0], :solution  6, :ratio 1}
                               {:cbi  0, :active-variable :s2, :constraint-coefficients [  4 0 -2/5 1], :solution 32, :ratio 10}]
   :cj-zj-row                 [12 16 0 0]
   :zj-row                    [0 0 0 0]
   :key-column-index          1
   :key-element               20
   :key-ratio-index           0
   :key-row-index             0
   :entering-variable         :x2
   :exiting-variable          :s1})

(def max-solution
  {:problem-type              :max,
   :iteration                 2,
   :basic-variable-row        [:x1 :x2 :s1 :s2],
   :objective-coeffecient-row [12 16 0 0],
   :tableaux-rows             [{:cbi 16, :active-variable :x2, :constraint-coefficients [0N 1N 1/10 -1/8], :solution 2N, :ratio 12N}
                               {:cbi 12, :active-variable :x1, :constraint-coefficients [1 0 -1/10 1/4], :solution 8, :ratio 1}],
   :zj-row                    [8N 16 4/5 0]
   :cj-zj-row                 [4N 0 -4/5 0],
   :key-column-index          0,
   :key-element               4,
   :key-ratio-index           1,
   :key-row-index             1,
   :entering-variable         :x1,
   :exiting-variable          :s2})

;; ======================
;; Min Problem Iterations
;; ======================

(def min-iteration-0-pre
  {:problem-type              :min
   :iteration                 0
   :basic-variable-row        [:x1 :y1 :s1 :s2 :s3]
   :objective-coeffecient-row [0.2 2 0 0 0] ;; cj from video
   :tableaux-rows             [{:cbi 0 :active-variable :s1 :constraint-coefficients [ 80  640 1 0 0] :solution  480 :ratio 0}
                               {:cbi 0 :active-variable :s2 :constraint-coefficients [  6   36 0 1 0] :solution   30 :ratio 0}
                               {:cbi 0 :active-variable :s3 :constraint-coefficients [600 1400 0 0 1] :solution 1600 :ratio 0}]})




(facts "Calculate ZJ Row Cases"
       (fact "Max: Given Iteration 0 we will correctly calculate a zero Zj row"
             (:zj-row (calculate-zj-row max-iteration-0-pre)) => [0 0 0 0])
       (fact "Min: Given Iteration 0 we will correctly calculate a zero Zj row"
             (:zj-row (calculate-zj-row min-iteration-0-pre)) => [0 0 0 0 0]))

(facts "Calculate Cj - Zj"
       (let [undertest (comp calculate-cj-zj-row calculate-zj-row)]
         (fact "Max: Given iteration 0 we will correctly calculate the Cj-Zj row"
               (:cj-zj-row (undertest max-iteration-0-pre)) => [12 16 0 0])
         (fact "Min: Given iteration 0 we will correctly calculate the Cj-Zj row"
               (:cj-zj-row (undertest min-iteration-0-pre)) => [0.2 2 0 0 0])))

(facts "Check Optimality"
       (let [undertest (comp optimal-solution? calculate-cj-zj-row calculate-zj-row)]
         (fact "Max: Given iteration 0 we will deduce that the max optimum has not been meet"
                 (undertest max-iteration-0-pre) => false)))

; NJ_TODO Re-add back in.
;(fact "Min: Given iteration 0 we will deduce that the max optimum has not been meet"
;      (undertest min-iteration-0-pre) => false)

(facts "Calculate Key Column"
       (fact "Max: Given iteration 0 we can correctly select the key column"
             (let [undertest (comp calculate-key-column calculate-cj-zj-row calculate-zj-row)
                   result    (undertest max-iteration-0-pre)]
               (:key-column-index result) => 1)))

(facts "Calculate Key Column Ratios"
       (fact "Max: Given iteration 0 we can correctly calculate the s / k ratios"
             (let [undertest (comp calculate-solution-to-key-val-ratio
                                   calculate-key-column
                                   calculate-cj-zj-row
                                   calculate-zj-row)
                   result    (undertest max-iteration-0-pre)]
               (:key-ratio-index result) => 0
               (:tableaux-rows result)   => [{:cbi 0, :active-variable :s1 :constraint-coefficients [10 20 1 0], :solution 120, :ratio  6}
                                             {:cbi 0, :active-variable :s2 :constraint-coefficients [8 8 0 1],   :solution  80, :ratio 10}])))


(facts "Calculate Key Row and Key Value"
       (fact "Max: Given iteration 0 we can correctly calculate the key row and key value"
             (let [undertest (comp calculate-key-row-and-value
                                   calculate-solution-to-key-val-ratio
                                   calculate-key-column
                                   calculate-cj-zj-row
                                   calculate-zj-row)
                   result    (undertest max-iteration-0-pre)]
               (:key-row-index result) => 0
               (:key-element   result) => 20)))


(facts "Calculate Entering and Exiting Variables"
       (fact "Max: Given iteration 0 with the key rows and columns calculated then we can calculate the exiting value."
             (let [undertest (comp calculate-entering-and-exiting-variables
                                   calculate-key-row-and-value
                                   calculate-solution-to-key-val-ratio
                                   calculate-key-column
                                   calculate-cj-zj-row
                                   calculate-zj-row)
                   result    (undertest max-iteration-0-pre)]
               (:tableaux-rows result)     => [{:cbi 0, :active-variable :s1 :constraint-coefficients [10 20 1 0], :solution 120, :ratio  6}
                                               {:cbi 0, :active-variable :s2 :constraint-coefficients [8 8 0 1],   :solution  80, :ratio 10}]
               (:exiting-variable result)  => :s1
               (:entering-variable result) => :x2
               result                      => max-iteration-0-post)))

(facts "Setup Next Iteration"
       (fact "Max: Given iteration 0 that has calculated entering and exiting variables then we can setup the next iteration."
             (let [undertest (comp setup-next-iteration
                                   calculate-entering-and-exiting-variables
                                   calculate-key-row-and-value
                                   calculate-solution-to-key-val-ratio
                                   calculate-key-column
                                   calculate-cj-zj-row
                                   calculate-zj-row)
                      result (undertest max-iteration-0-pre)]
               result => max-iteration-1-pre)))

(facts "Full Simplex Check"
       (fact "Max: Check that we can calculate a correct solution given iteration 0"
             (last (simplex max-iteration-0-pre)) => max-solution))

(facts "Tableaux Solution To String"
       (fact "Max: Full Simplex Solution can generate correct short form String"
             (tableaux-solution-to-string (last (simplex max-iteration-0-pre))) => "x1 = 8, x2 = 2"))

;; ======================
;;   Helper Function Tests
;; ======================

(facts "Calculate Entering Row should use the key element (ke) to create a ratio of old coeffecient over ke"
       (fact "Given a well formed row the function will correctly calculate the new ratios"
             (let [input       {:cbi 16, :active-variable :x1, :constraint-coefficients [10 20 1 0], :solution 120, :ratio 6}
                   key-element 20]
               (calculate-entering-row input key-element) => {:cbi 16, :active-variable :x1, :constraint-coefficients [1/2 1 1/20 0], :solution 6, :ratio 1})))

(facts "Calculate Non Entering Rows will update all non key rows ready for next iteration."
       (fact "Given a well formed vector of rows the function can correctly calculate new rows"
             (let [previous-key-row {:cbi 0, :active-variable :s1 :constraint-coefficients [10 20 1 0], :solution 120, :ratio  6}
                   key-element      20
                   key-row-index    0
                   input            [{:cbi 16, :active-variable :x1, :constraint-coefficients [1/2 1 1/20 0], :solution 6, :ratio 1}
                                     {:cbi 0, :active-variable :s2, :constraint-coefficients [8 8 0 1], :solution 80, :ratio 10}]]
               (calculate-non-entering-rows input previous-key-row key-row-index 0 key-element) => [{:cbi 16, :active-variable :x1, :constraint-coefficients [1/2 1 1/20 0], :solution  6, :ratio 1}
                                                                                                    {:cbi  0, :active-variable :s2, :constraint-coefficients [  4 0 -2/5 1], :solution 32, :ratio 10}])))

;; =================
;; Dual Form Example
;; =================

(def dual-form-iteration-0-pre
  {:problem-type              :min
   :iteration                 0
   :basic-variable-row        [:x1 :y1 :s1 :s2]
   :objective-coeffecient-row [14 20 0 0] ;; cj from video
   :tableaux-rows             [{:cbi 0 :active-variable :s1 :constraint-coefficients [1 2 1 0] :solution  4 :ratio 0}
                               {:cbi 0 :active-variable :s2 :constraint-coefficients [7 6 0 1] :solution 20 :ratio 0}]})

(def dual-form-iteration-0-post
  {:problem-type              :max
   :iteration                 0
   :basic-variable-row        [:x1 :y1 :s1 :s2]
   :objective-coeffecient-row [4 20 0 0] ;; cj from video
   :tableaux-rows             [{:cbi 0 :active-variable :s1 :constraint-coefficients [1 7 1 0] :solution 14 :ratio 0}
                               {:cbi 0 :active-variable :s2 :constraint-coefficients [2 6 0 1] :solution 20 :ratio 0}]})


(facts "Calculate Dual Form Of Minimization Problem"
       (fact "Dual form conversion is successful for minimisation problem of 2 variables and 2 constraints."
             (calculate-obj-cons-transpose-for-dual-form dual-form-iteration-0-pre) => [[1 7 14]
                                                                                        [2 6 20]
                                                                                        [4 20 1]]
             (construct-dual-form-of-tableaux dual-form-iteration-0-pre) => dual-form-iteration-0-post)
       (fact "Dual form conversion is successful for minimisation problem of 2 variables and 3 constraints."
             (construct-dual-form-of-tableaux min-iteration-0-pre) => dual-form-iteration-0-post))

;; ======================
;;     Zombie Cases
;; ======================
