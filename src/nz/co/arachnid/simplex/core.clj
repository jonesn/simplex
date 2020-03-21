(ns nz.co.arachnid.simplex.core
  (:require [clojure.spec.alpha :as s]
            [clojure.pprint     :as pp]
            [clojure.string     :as str]
            [nz.co.arachnid.simplex.tableaux-printer :refer :all]))

;; Referencing Video
;; https://www.youtube.com/watch?v=M8POtpPtQZc&list=PLhL0OLgFT2BSx6XvhpWmlzOO2kOR82dQK&index=2&t=0s

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

;; Max z: 12x1 + 16x2 + 0s1 + 0s2 so: 12, 16, 0, 0
;; All co-effecients from the slack variable form of the objective or constraint functions.
;; Max z: 12x1 + 16x2 + 0s1 + 0s2 so: x1, x2, s1, s2
;; The variables from the slack variable form of the objective or constraint function.

;; Zj Row
;; ======

;; Zj = sum of i = 1..n (cbi[i] * constraint-coeffecients[i,j]
;; n is the number of (constraint coeffecients + solution).
;; Zj = [0*10 + 0*8, 0*20+0*8, 0*1+0*0, 0*0+0*1, 0*120+0*80]

;; Maximization Problem
;; ====================
;; all Cj - Zj <= 0

;; Minimization Problem
;; ====================
;; all Cj - Zj >= 0

;; =====================================
;;                Specs
;; =====================================

(s/check-asserts true)

(s/def ::problem-type (hash-set :min :max))
(s/def ::iteration    nat-int?)
(s/def ::basic-variable-row (s/coll-of keyword? :kind vector? :distinct true :into []))
(s/def ::objective-coeffecient-row (s/coll-of number? :kind vector? :into []))
(s/def ::cbi number?)
(s/def ::active-variable keyword?)
(s/def ::constraint-coefficients (s/coll-of number? :kind vector? :into []))
(s/def ::solution number?)
(s/def ::ratio number?)
(s/def ::zj-row (s/coll-of number? :kind vector? :into []))
(s/def ::cj-zj-row (s/coll-of number? :kind vector? :into []))
(s/def ::key-column-index nat-int?)
(s/def ::key-element number?)
(s/def ::key-ratio-index nat-int?)
(s/def ::key-row-index nat-int?)
(s/def ::entering-variable keyword?)
(s/def ::exiting-variable keyword?)

(s/def ::tableaux-row (s/keys :req-un [::cbi ::active-variable ::constraint-coefficients ::solution ::ratio]))
(s/def ::tableaux-rows (s/coll-of ::tableaux-row :kind vector? :into []))
(s/def ::tableaux (s/keys :req-un [::problem-type
                                   ::iteration
                                   ::basic-variable-row
                                   ::objective-coeffecient-row
                                   ::tableaux-rows]
                          :opt-un [::zj-row
                                   ::cj-zj-row
                                   ::key-column-index
                                   ::key-element
                                   ::key-ratio-index
                                   ::entering-variable
                                   ::exiting-variable]))

;; ========================
;; Helper Functions Private
;; ========================

(defn- positions
  [pred coll]
  (keep-indexed (fn [idx x]
                  (when (pred x)
                    idx))
                coll))

(defn- mult-coeffecients-by-scalar
  [n vec-coeffecients]
  (map
    (fn [elem] (* n elem))
    vec-coeffecients))

(defn- transpose
  "Takes a vector of equal length vectors and produces its transpose.
   Example:
   ========
   (transpose [[1 2] [3 4]]) => [[1 3] [2 4]]"
  [m]
  (apply mapv vector m))

;; =======================
;; Helper Functions Public
;; =======================

(defn tableaux-solution-to-string
  "Function takes an entire solved tableaux and returns the solved variables in the form:
   x1 = 8, x2 = 2"
  [tableaux]
  (let [tableaux-rows (:tableaux-rows tableaux)
        var-to-sol    (map
                        (fn [row] (vals (select-keys row [:active-variable :solution])))
                        tableaux-rows)]
    (->> var-to-sol
         ;; Filter out any variables the don't start with x
         (filter (fn [pair] (str/starts-with? (name (first pair)) "x")))
         ;; Form the String of form x1=3
         (map    (fn [pair] (str (name (first pair)) " = " (second pair))))
         (sort)
         (str/join ", "))))


(defn calculate-obj-cons-transpose-for-dual-form
  "Constructs the transpose matrix for the dual form of the given Tableaux. Returns a nested vector of the form:
   ```clojure
   [[1 7 14]
    [2 6 20]
    [4 20 1]]
   ```
   For full details of the dual form see: [[construct-dual-form-of-tableaux]]"
  [tableaux]
  (let [tableaux-rows               (:tableaux-rows tableaux)
        all-constraint-coeffecients (mapv :constraint-coefficients tableaux-rows)
        ;; Count the number of non slack variables
        number-of-non-slack-vars    (count
                                     (filter
                                      (fn [b-var]
                                        (not (str/starts-with? (name b-var) "s")))
                                      (:basic-variable-row tableaux)))
        objective-coeffecients-sol  (conj
                                     (vec (take number-of-non-slack-vars (:objective-coeffecient-row tableaux)))
                                     1)
        ;; We only want to transpose non slack coeffecients
        constraint-coeffecients     (mapv
                                     (fn [constraints] (vec (take number-of-non-slack-vars constraints)))
                                     all-constraint-coeffecients)
        constraint-solutions        (mapv :solution tableaux-rows)
        constraint-coeff-and-sols   (mapv
                                     (fn [constraints solution] (conj constraints solution))
                                     constraint-coeffecients
                                     constraint-solutions)]
    (transpose
      (conj constraint-coeff-and-sols objective-coeffecients-sol))))


(defn replace-with-dual-coeff
  "Replaces the head, defined by number of variables, of the old vector with that of the new."
  [old new number-of-variables]
  (vec
    (concat (butlast new)
            (nthrest old number-of-variables))))


(defn calculate-entering-row
  [old-row key-element]
  (let [safe-key-element-denomitator (if (= key-element 0) 1 key-element)
        constraint-coefficients      (:constraint-coefficients old-row)
        new-coeffecients             (mapv (fn [x] (/ x safe-key-element-denomitator)) constraint-coefficients)
        new-solution                 (/ (:solution old-row) safe-key-element-denomitator)]
    (merge old-row {:constraint-coefficients new-coeffecients
                    :solution                new-solution
                    :ratio                   1})))


(defn calculate-non-entering-value
  [old-val corresponding-key-col-val corresponding-key-row-val key-element]
  (let [safe-key-element-denominator (if (= key-element 0) 1 key-element)]
    (- old-val
       (/ (* corresponding-key-col-val corresponding-key-row-val)
          safe-key-element-denominator))))


(defn calculate-non-entering-row
  [tableaux-row
   previous-iteration-key-row
   key-row-index
   key-column-index
   current-row-index
   key-element]
  (if (= current-row-index key-row-index)
    ;; then
    tableaux-row
    ;; else
    (let [current-constraint-coefficients (:constraint-coefficients tableaux-row)
          key-row-coefficients            (:constraint-coefficients previous-iteration-key-row)
          updated-coefficients            (vec
                                            (map-indexed
                                              (fn [index element]
                                                (calculate-non-entering-value
                                                  element
                                                  (nth current-constraint-coefficients key-column-index)
                                                  (nth key-row-coefficients index)
                                                  key-element))
                                              current-constraint-coefficients))
          current-solution                (:solution tableaux-row)
          key-row-solution                (:solution previous-iteration-key-row)
          updated-key-row                 (calculate-non-entering-value
                                            current-solution
                                            (nth current-constraint-coefficients key-column-index)
                                            key-row-solution
                                            key-element)]
      (merge tableaux-row {:constraint-coefficients updated-coefficients
                           :solution                updated-key-row}))))


(defn calculate-non-entering-rows
  [tableaux-rows previous-iteration-key-row key-row-index key-column-index key-element]
  (vec
    (map-indexed
      (fn [current-row-index row]
        (calculate-non-entering-row
          row
          previous-iteration-key-row
          key-row-index
          key-column-index
          current-row-index
          key-element))
      tableaux-rows)))

;; ================
;;  Main Functions
;; ================

(defn calculate-zj-row
  "Zj Row
   ======
   - Zj = sum of i = 1..n (cbi[i] * constraint-coeffecients[i,j]
     n is the number of (constraint coeffecients + solution)
   - The supplied tableaux is updated with the result
   ## New Keys:
   - :Zj-row"
  [tableaux]
  (let [t-rows           (:tableaux-rows tableaux)
        cbi*constraints  (mapv
                           (fn [row] (mult-coeffecients-by-scalar (:cbi row) (:constraint-coefficients row)))
                           t-rows)
        zj               (apply mapv + cbi*constraints)]
    (assoc tableaux :zj-row zj)))


(defn calculate-cj-zj-row
  "Cj - Zj Row
   ===========
   - Once the Zj row is calculated simply subtracts the Cj row from it.
     The supplied tableaux is updated with the result
   ## New Keys
   - :Cj-Zj"
  [tableaux]
  (let [zj-row   (:zj-row tableaux)
        cj-row   (:objective-coeffecient-row tableaux)
        calc-row (mapv - cj-row zj-row)]
    (merge tableaux {:cj-zj-row calc-row})))


(defn optimal-solution?
  "- For max problems all Cj-Zj <= 0
   - For min problems all Cj-Zj >= 0"
  [tableaux]
  (true?
    (when-let [cj-zj-row (:cj-zj-row tableaux)]
      (or
        (and (= (:problem-type tableaux) :min) (every? (fn [x] (>= x 0)) cj-zj-row))
        (and (= (:problem-type tableaux) :max) (every? (fn [x] (<= x 0)) cj-zj-row))))))


(defn find-key-value
  "The key value is the maximum value of the :cj-zj-row whose column in the
   tableaux is not that of an active basic variable."
  [selection-fn cj-zj-row basic-variables active-basic-variables]
  (let [active-positions (positions (set active-basic-variables) basic-variables)
        ;; Extract and name this function: remove-elements-at-index
        non-active-cj-zj (filterv #(not (nil? %))
                                  (map-indexed
                                    (fn [index cj-zj-element]
                                      (when (not (some #{index} active-positions))
                                        cj-zj-element))
                                    cj-zj-row))]
    (apply selection-fn non-active-cj-zj)))


(defn find-key-column-value
  [tableaux]
  (let [tableaux-rows         (:tableaux-rows tableaux)
        basic-variables       (:basic-variable-row tableaux)
        active-variables      (mapv :active-variable tableaux-rows)
        cj-zj-row             (:cj-zj-row tableaux)]
    (find-key-value max cj-zj-row basic-variables active-variables)))


(defn calculate-key-column
  "- Fox max problems the key column is the cj-zj-row column containing the highest value.
   ## New Keys:
   - :key-column-index"
  [tableaux]
  (let [cj-zj-row     (:cj-zj-row tableaux)
        key-value     (find-key-column-value tableaux)
        column-index  (first (positions #{key-value} cj-zj-row))]
    (assoc tableaux :key-column-index column-index)))


(defn calculate-solution-to-key-val-ratio
  "- Take the solution (s) and key (k) columns and produce a new column of the ratios: s / k. The minimum
     of these ratios is selected to help us identify the leaving basic variable in subsequent steps.
   - The ratios in the Tableaux rows will be updated
   - Only positive coefficients in the key column will be considered.
   ## New Keys:
   - key-ratio-index"
  [tableaux]
  (let [tableaux-rows         (:tableaux-rows tableaux)
        solution-column       (map :solution tableaux-rows)
        key-column-index      (:key-column-index tableaux)
        key-column            (->> (:tableaux-rows tableaux)
                                   (map :constraint-coefficients)
                                   (mapv (fn [v] (nth v key-column-index))))
        ratios                (mapv
                               (fn [x y] (if (= 0 y)
                                           x
                                           (/ x y)))
                               solution-column
                               key-column)
        updated-tableaux-rows (mapv (fn [map val] (assoc map :ratio val)) tableaux-rows ratios)
        key-ratio-value       (apply min (filter pos? ratios))
        key-ratio-index       (first (positions #{key-ratio-value} ratios))]
    (merge
      tableaux
      {:tableaux-rows   updated-tableaux-rows
       :key-ratio-index key-ratio-index})))


(defn calculate-key-row-and-value
  "- Selects the intersection of the key column and row indexes and sets that as the key value.
     The selection of the key row is based on the ratio index.
   ## New Keys:
   - :key-row-index
   - :key-value"
  [tableaux]
  (let [key-ratio-index  (:key-ratio-index tableaux)
        key-row          (:constraint-coefficients (nth (:tableaux-rows tableaux) key-ratio-index))
        key-column-index (:key-column-index tableaux)
        key-value        (nth key-row key-column-index)]
    (merge
      tableaux
      {:key-row-index key-ratio-index
       :key-element   key-value})))


(defn calculate-entering-and-exiting-variables
  "- Here we clearly extract the entering and exiting variables for the next iteration.
     The entering variable is variable in vector :basic-variables that indexed by :key-column-index.
     The exiting variable is the :active-variable indexed by :key-row-index
     ## New Keys:
     - :entering-variable
     - :exiting-variable"
  [tableaux]
  (let [key-column-index      (:key-column-index tableaux)
        basic-variable-row    (:basic-variable-row tableaux)
        entering-variable     (nth basic-variable-row key-column-index)
        key-row-index         (:key-row-index tableaux)
        tableaux-rows         (:tableaux-rows tableaux)
        exiting-variable      (:active-variable (nth tableaux-rows key-row-index))]
    (merge tableaux {:entering-variable entering-variable
                     :exiting-variable  exiting-variable})))


(defn setup-next-iteration
  "This function will setup the next iteration of the Tableaux. It is the most computationally
   expensive step as it updates all the rows in the tableaux with new values based on the:
    - :key-row
    - :key-column
    - :key-element"
  [tableaux]
  (let [key-element                (:key-element tableaux)
        key-column-index           (:key-column-index tableaux)
        key-row-index              (:key-row-index tableaux)
        tableaux-rows              (:tableaux-rows tableaux)
        entering-variable          (:entering-variable tableaux)
        coeffecient-row            (:objective-coeffecient-row tableaux)
        entering-coeffecient       (nth coeffecient-row key-column-index)
        entering-row-to-update     (nth tableaux-rows key-row-index)
        updated-entering-row-step1 (merge entering-row-to-update
                                          {:active-variable entering-variable
                                           :cbi             entering-coeffecient})
        updated-entering-row-step2 (calculate-entering-row updated-entering-row-step1 key-element)
        updated-tableaux-rows-with-entering-row (assoc tableaux-rows key-row-index updated-entering-row-step2)
        updated-all-rows           (calculate-non-entering-rows
                                     updated-tableaux-rows-with-entering-row
                                     entering-row-to-update
                                     key-row-index
                                     key-column-index
                                     key-element)]
       (merge tableaux {:tableaux-rows updated-all-rows
                        :iteration     (inc (:iteration tableaux))})))


(defn construct-dual-form-of-tableaux
  "To solve minimization problems using Simplex we can reframe the initial problem as
   a maximisation problem. This is achieved by taking the dual form which involves
   taking the transpose of the constraints and objective functions.
   Example:
   ========
   Minimise: 14s + 20t = C
   Subject to:
             s + 2t  >= 4
             7s + 6t >= 20

   Appending the objective function (c = 1) as the last row gives the Matrix:
   [ 1  2  4
     7  6 20
    14 20  1]

   Equivalent Dual Form Maximisation Problem:
   ==========================================

   Using different variables to highlight the change:

   Maximise: 4x + 20y = P
   Subject To:
             x + 7y <= 14
            2x + 6y <= 20

   [1  7 14
    2  6 20
    4 20  1]

    Hence, optimal solution is arrived with value of variables as:
     - x=2,y=1, Max Z=48
    So given the duality:
     - s=2,t=1, Min C=48
   "
  [tableaux]
  (let [transpose-for-dual-form    (calculate-obj-cons-transpose-for-dual-form tableaux)
        tableaux-rows              (:tableaux-rows tableaux)
        objective-row              (:objective-coeffecient-row tableaux)
        dual-form-constraint-vec   (butlast transpose-for-dual-form)
        dual-form-objective-vec    (last transpose-for-dual-form)
        number-of-variables        (count (butlast dual-form-objective-vec))
        updated-objective-row      (replace-with-dual-coeff objective-row dual-form-objective-vec number-of-variables)
        updated-tableaux-rows      (mapv (fn [t-row dual-form-vec]
                                           (let [constraint-coefficients         (:constraint-coefficients t-row)
                                                 updated-constraint-coefficients (replace-with-dual-coeff constraint-coefficients dual-form-vec number-of-variables)
                                                 updated-solution                (last dual-form-vec)]
                                             (merge t-row {:constraint-coefficients updated-constraint-coefficients
                                                           :solution                updated-solution})))
                                         tableaux-rows
                                         dual-form-constraint-vec)]
    (merge tableaux {:problem-type              :max
                     :objective-coeffecient-row updated-objective-row
                     :tableaux-rows             updated-tableaux-rows})))


(defn transform-to-standard-form
  "Construct a standard form of the given Tableaux. This involves framing the
   problem as a maximisation problem and having all constraints in <= form."
  [tableaux]
  (case (:problem-type tableaux)
    :min (construct-dual-form-of-tableaux tableaux)
    :max tableaux))


(defn simplex
  "Recursive implementation that runs the full simplex algorithm for a valid initial Tableaux.
   It will start by checking for optimality and if it is not meet will process another iteration.
   ## Returns
   A vector containing each Tableaux iteration"
  ;; Arity 1
  ([tableaux]
   (let [validated-tableaux (s/assert ::tableaux tableaux)]
     (simplex validated-tableaux [validated-tableaux] 20)))
  ;; Arity 2
  ([tableaux iterations max-iterations]
   (let [standard-form     (transform-to-standard-form tableaux)
         optimality-fn     (fn [t]
                             (->> t
                                  calculate-zj-row
                                  calculate-cj-zj-row))
         full-iteration-fn (fn [t] (->> t
                                        optimality-fn
                                        calculate-key-column
                                        calculate-solution-to-key-val-ratio
                                        calculate-key-row-and-value
                                        calculate-entering-and-exiting-variables
                                        setup-next-iteration))]
     (cond (optimal-solution? (optimality-fn standard-form)) iterations
           ;; concrete max iterations for now
           (> (count iterations) max-iterations)             iterations
           :default                                          (let [next-iteration     (s/assert ::tableaux (full-iteration-fn standard-form))
                                                                   updated-iterations (conj iterations next-iteration)]
                                                               (simplex next-iteration updated-iterations max-iterations))))))

;; ======================================
;;        Comment Helper Functions
;; ======================================

(comment (def it0 {:problem-type              :max
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
         (def tab1 (calculate-zj-row it0))
         (def tab2 (calculate-cj-zj-row it0))
         (find-key-column-value it0)
         (def calc-fn1 (comp calculate-key-column calculate-cj-zj-row calculate-zj-row))
         (def calc-fn2 (comp calculate-entering-and-exiting-variables
                             calculate-key-row-and-value
                             calculate-solution-to-key-val-ratio
                             calculate-key-column
                             calculate-cj-zj-row
                             calculate-zj-row))
         (calc-fn1 it0)
         (calc-fn2 it0)
         (clojure.pprint/pprint tab2)
         (reduce apply #(mapv (fn [x] (* 2 x)) %1) [[1 2] [3 4]])
         (def rows-to-reduce [{:cbi 16, :active-variable :x1, :constraint-coefficients [1/2 1 1/20 0], :solution 6, :ratio 1}
                              {:cbi 0, :active-variable :s2, :constraint-coefficients [8 8 0 1], :solution 80, :ratio 10}])
         (calculate-non-entering-row
           {:cbi 0, :active-variable :s2, :constraint-coefficients [8 8 0 1], :solution 80, :ratio 10}
           {:cbi 0, :active-variable :s1, :constraint-coefficients [10 20 1 0], :solution 120, :ratio 0}
           0
           1
           1
           20)
         (defn positive-even-numbers
           ([] (positive-even-numbers 2))
           ([n] (lazy-seq (cons n (positive-even-numbers (+ n 2))))))
         (calculate-non-entering-rows [8 8 0 1] [10 20 1 0] 0 1 20)
         (def sol (simplex it0))
         (optimal-solution? it0)
         (def min-iteration-0-pre
           {:problem-type              :min
            :iteration                 0
            :basic-variable-row        [:x1 :y1 :s1 :s2 :s3]
            :objective-coeffecient-row [-0.2 -2 0 0 0] ;; cj from video
            :tableaux-rows             [{:cbi 0 :active-variable :s1 :constraint-coefficients [ 80  640 1 0 0] :solution  480 :ratio 0}
                                        {:cbi 0 :active-variable :s2 :constraint-coefficients [  6   36 0 1 0] :solution   30 :ratio 0}
                                        {:cbi 0 :active-variable :s3 :constraint-coefficients [600 1400 0 0 1] :solution 1600 :ratio 0}]})
         (def dual-form-iteration-0-pre
           {:problem-type              :min
            :iteration                 0
            :basic-variable-row        [:x1 :y1 :s1 :s2]
            :objective-coeffecient-row [14 20 0 0] ;; cj from video
            :tableaux-rows             [{:cbi 0 :active-variable :s1 :constraint-coefficients [1 2 1 0] :solution  4 :ratio 0}
                                        {:cbi 0 :active-variable :s2 :constraint-coefficients [7 6 0 1] :solution 20 :ratio 0}]})
         (construct-dual-form-of-tableaux dual-form-iteration-0-pre)
         (simplex it0)
         (last (simplex it0))
         (print-results (simplex it0) "/tmp/simplex-output.html")
         (apply #(mapv + %1 %2) [[1 2] [3 4]])
         (apply mapv + [[1 2 5] [3 4 6] [5 6 7]])
         (map-indexed
           (fn [index element]
             (* index element)) [1 2 3 4 5]))
