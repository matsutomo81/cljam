(ns cljam.algo.dedupe-test
  (:require [clojure.test :refer [deftest is]]
            [cljam.test-common :as common]
            [cljam.algo.dedupe :as dedupe]))

(deftest simple-pe-dedupe
  (common/with-before-after {:before (common/prepare-cache!)
                             :after (common/clean-cache!)}
    (let [out-file (str common/temp-dir "/deduped.bam")]
      (is (common/not-throw?
           (dedupe/dedupe common/dedupe-before-bam-file out-file)))
      (is (common/same-sam-contents? out-file common/dedupe-after-bam-file)))))

(deftest simple-pe-dedupe-xform
  (is (= (into #{}
               (dedupe/dedupe-xform)
               [{:qname "r1" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIII"}
                {:qname "r2" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIIH"}
                {:qname "r1" :flag 147 :rname "ref" :pos 10 :mapq 60 :rnext "=" :pnext 1 :tlen -5 :qual "IIIII"}])
         #{{:qname "r1" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIII"}
           {:qname "r1" :flag 147 :rname "ref" :pos 10 :mapq 60 :rnext "=" :pnext 1 :tlen -5 :qual "IIIII"}}))

  (is (= (into #{}
               (dedupe/dedupe-xform)
               [{:qname "r1" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIIH"}
                {:qname "r2" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIII"}
                {:qname "r1" :flag 147 :rname "ref" :pos 10 :mapq 60 :rnext "=" :pnext 1 :tlen -5 :qual "IIIII"}])
         #{{:qname "r2" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIII"}}))

  (is (= (into #{}
               (dedupe/dedupe-xform)
               [{:qname "r1" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIII"}
                {:qname "r2" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 11 :tlen 6 :qual "IIIIH"}
                {:qname "r1" :flag 147 :rname "ref" :pos 10 :mapq 60 :rnext "=" :pnext 1 :tlen -5 :qual "IIIII"}
                {:qname "r2" :flag 147 :rname "ref" :pos 11 :mapq 60 :rnext "=" :pnext 1 :tlen -6 :qual "IIIII"}])
         #{{:qname "r1" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIII"}
           {:qname "r2" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 11 :tlen 6 :qual "IIIIH"}
           {:qname "r1" :flag 147 :rname "ref" :pos 10 :mapq 60 :rnext "=" :pnext 1 :tlen -5 :qual "IIIII"}
           {:qname "r2" :flag 147 :rname "ref" :pos 11 :mapq 60 :rnext "=" :pnext 1 :tlen -6 :qual "IIIII"}}))

  (is (= (into #{}
               (dedupe/dedupe-xform)
               [{:qname "r1" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIII"}
                {:qname "r2" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIIH"}
                {:qname "r4" :flag 4 :rname "ref" :pos 1 :mapq 0 :rnext "*" :pnext 0 :tlen 0 :qual "IIIII"}
                {:qname "r1" :flag 147 :rname "ref" :pos 10 :mapq 60 :rnext "=" :pnext 1 :tlen -5 :qual "IIIII"}
                {:qname "r2" :flag 147 :rname "ref" :pos 10 :mapq 60 :rnext "=" :pnext 1 :tlen -5 :qual "IIIII"}
                {:qname "r3" :flag 99 :rname "ref" :pos 10 :mapq 60 :rnext "ref2" :pnext 1 :tlen 0 :qual "IIIII"}
                {:qname "r3" :flag 147 :rname "ref2" :pos 1 :mapq 60 :rnext "ref" :pnext 10 :tlen 0 :qual "IIIII"}])
         #{{:qname "r4" :flag 4 :rname "ref" :pos 1 :mapq 0 :rnext "*" :pnext 0 :tlen 0 :qual "IIIII"}
           {:qname "r1" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIII"}
           {:qname "r3" :flag 99 :rname "ref" :pos 10 :mapq 60 :rnext "ref2" :pnext 1 :tlen 0 :qual "IIIII"}
           {:qname "r1" :flag 147 :rname "ref" :pos 10 :mapq 60 :rnext "=" :pnext 1 :tlen -5 :qual "IIIII"}
           {:qname "r3" :flag 147 :rname "ref2" :pos 1 :mapq 60 :rnext "ref" :pnext 10 :tlen 0 :qual "IIIII"}}))

  (is (= (into #{}
               (dedupe/dedupe-xform :remove-dups false)
               [{:qname "r1" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIII"}
                {:qname "r2" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIIH"}
                {:qname "r1" :flag 147 :rname "ref" :pos 10 :mapq 60 :rnext "=" :pnext 1 :tlen -5 :qual "IIIII"}])
         #{{:qname "r1" :flag 99 :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIII"}
           {:qname "r2" :flag (+ 99 1024) :rname "ref" :pos 1 :mapq 60 :rnext "=" :pnext 10 :tlen 5 :qual "IIIIH"}
           {:qname "r1" :flag 147 :rname "ref" :pos 10 :mapq 60 :rnext "=" :pnext 1 :tlen -5 :qual "IIIII"}})))
