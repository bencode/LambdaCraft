^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.branchless
  (:require [nextjournal.clerk :as clerk]
            [clojure.java.io :as io]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md (slurp (io/file "pages/md/branchless.md")))
