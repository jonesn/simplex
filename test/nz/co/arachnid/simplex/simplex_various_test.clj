(ns nz.co.arachnid.simplex.simplex-various-test
  (:require [midje.sweet :refer :all])
  (:require [nz.co.arachnid.simplex.core :refer :all]))

;; =================================================================================
;; This namespace allows for the collection of adhoc tests for use for verification
;; =================================================================================

;; Max Simple 2 Variable - Drugs and Machines
;; ==========================================

;; A company produces drugs A and B using machines M 1 and M 2.
;; - 1 ton of drug A requires 1 hour of processing on M1 and 2 hours on M2
;; - 1 ton of drug B requires 3 hours of processing on M1 and 1 hour on M2
;; - 9 hours of processing on M1 and 8 hours on M2 are available each day
;; - Each ton of drug produced (of either type) yields £1 million profit

;; x 1 = number of tons of A produced
;; x 2 = number of tons of B produced

;; Maximise:
;; x1 + x2 (profit in £ million)

;; Subject To:
;; x1 + 3x2 <= 9 (M1 processing)
;; 2x1 + x2 <= 8 (M2 processing)
;; x1, x2 > 0

(def max-iteration-pre-drugs-machines
  {:problem-type              :max
   :iteration                 0
   :basic-variable-row        [:x1 :x2 :s1 :s2]
   :objective-coeffecient-row [1 1 0 0] ;; cj from video
   :tableaux-rows             [{:cbi 0 :active-variable :s1 :constraint-coefficients [1 3 1 0] :solution 9 :ratio 0}
                               {:cbi 0 :active-variable :s2 :constraint-coefficients [2 1 0 1] :solution 8 :ratio 0}]})

(facts "Drugs Machine Solution To String"
       (fact "Full Simplex Solution can generate correct short form String"
             (tableaux-solution-to-string (simplex max-iteration-pre-drugs-machines)) => "x1 = 3, x2 = 2"))