(ns nz.co.arachnid.simplex.parser-test
  (:require [nz.co.arachnid.simplex.parser :refer :all])
  (:require [midje.sweet                   :refer :all]))

(facts "Check constraint transformations to augmented form."
       (fact "Standard form transform >= to slack and equals."
             (slack-form-of-constraint (constraint-parser "6x1+9x2>=1000") "s1") => (constraint-parser "6x1+9x2+s1=1000")))

