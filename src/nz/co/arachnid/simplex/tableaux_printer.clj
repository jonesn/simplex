(ns nz.co.arachnid.simplex.tableaux-printer
  (:require [selmer.parser :as sp]))

(sp/set-resource-path! "templates/")

(defn print-results
  [iterations output-path]
  (spit
    output-path
    (sp/render-file "samplesimplex.html" {:iterations iterations})))


