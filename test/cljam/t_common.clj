(ns cljam.t-common
  (:use [clojure.java.io :only [file]])
  (:require [cljam.sam :as sam]
            [cljam.bam :as bam]
            [cljam.io :as io]
            [cavy.core :as cavy :refer [defcavy]]
            ))

;;; 一時メモ:
;;; cavyを使う上で、以下を決定する必要がある
;;; - 利用するファイルの選別
;;;   - B6_all_bwa.sorted.bam そのままでいいと思う
;;;     - auth対応待ち
;;;     - JVM_OPTS等の指定も追加したいところ(lein midjeだけで有効にできるか？)
;;; - 利用するtest
;;;   - これについては、今medium.bam使ってるところ全部でいいと思う
;;; とりあえず今は試験として、t_bam.cljにのみ導入してみる事に

(defcavy mycavy
  {:resources [{:id "large.bam"
                ;:url "https://share.xcoo.jp/works/cira/ChIP-Seq2/MBD-seq/B6_all_bwa.sorted.bam"
                ;:sha1 "de7604b60a894d8506405654f40c733879c48030"
                :url "http://misc.tir.jp/tmp/mini-test.bam"
                :sha1 "a5d2701e0d55e5943453890849eb2900b4b75da7"
                }
               ]})

(defn prepare-cavy! [] (cavy/without-print (cavy/get)) (cavy/verify))
(defn clean-cavy! [] (cavy/clean))

;;; slurp (for test)
(defn slurp-sam-for-test [f]
  (with-open [r (sam/reader f)]
    {:header (io/read-header r)
     :alignments (doall (io/read-alignments r {}))}))

(defn slurp-bam-for-test [f]
  (with-open [r (bam/reader f)]
    {:header (io/read-header r)
     :alignments (doall (io/read-alignments r {}))}))

;; spit (for test)
(defn spit-sam-for-test [f sam]
  (with-open [w (sam/writer f)]
    (io/write-header w (:header sam))
    (io/write-alignments w (:alignments sam) nil)))

(defn spit-bam-for-test [f sam]
  (with-open [w (bam/writer f)]
    (io/write-header w (:header sam))
    (io/write-refs w (:header sam))
    (io/write-alignments w (:alignments sam) (:header sam))))

(def test-sam-file "test/resources/test.sam")
(def test-bam-file "test/resources/test.bam")
(def test-sorted-bam-file "test/resources/test.sorted.bam")
(def test-fa-file  "test/resources/test.fa")
(def test-fai-file "test/resources/test.fa.fai")

(def test-sam
  {:header {:SQ [{:SN "ref", :LN 45} {:SN "ref2", :LN 40}]}
   :alignments
   [{:qname "r003", :flag 16 , :rname "ref" , :pos 29, :mapq 30, :cigar "6H5M"              , :rnext "*", :pnext 0 , :tlen  0  , :seq "TAGGC"                     , :qual "*"                         , :options []}
    {:qname "r001", :flag 163, :rname "ref" , :pos 7 , :mapq 30, :cigar "8M4I4M1D3M"        , :rnext "=", :pnext 37, :tlen  39 , :seq "TTAGATAAAGAGGATACTG"       , :qual "*"                         , :options [{:XX {:type "B", :value "S,12561,2,20,112"}}]}
    {:qname "r002", :flag 0  , :rname "ref" , :pos 9 , :mapq 30, :cigar "1S2I6M1P1I1P1I4M2I", :rnext "*", :pnext 0 , :tlen  0  , :seq "AAAAGATAAGGGATAAA"         , :qual "*"                         , :options []}
    {:qname "r003", :flag 0  , :rname "ref" , :pos 9 , :mapq 30, :cigar "5H6M"              , :rnext "*", :pnext 0 , :tlen  0  , :seq "AGCTAA"                    , :qual "*"                         , :options []}
    {:qname "x3"  , :flag 0  , :rname "ref2", :pos 6 , :mapq 30, :cigar "9M4I13M"           , :rnext "*", :pnext 0 , :tlen  0  , :seq "TTATAAAACAAATAATTAAGTCTACA", :qual "??????????????????????????", :options []}
    {:qname "r004", :flag 0  , :rname "ref" , :pos 16, :mapq 30, :cigar "6M14N1I5M"         , :rnext "*", :pnext 0 , :tlen  0  , :seq "ATAGCTCTCAGC"              , :qual "*"                         , :options []}
    {:qname "r001", :flag 83 , :rname "ref" , :pos 37, :mapq 30, :cigar "9M"                , :rnext "=", :pnext 7 , :tlen  -39, :seq "CAGCGCCAT"                 , :qual "*"                         , :options []}
    {:qname "x1"  , :flag 0  , :rname "ref2", :pos 1 , :mapq 30, :cigar "20M"               , :rnext "*", :pnext 0 , :tlen  0  , :seq "AGGTTTTATAAAACAAATAA"      , :qual "????????????????????"      , :options []}
    {:qname "x2"  , :flag 0  , :rname "ref2", :pos 2 , :mapq 30, :cigar "21M"               , :rnext "*", :pnext 0 , :tlen  0  , :seq "GGTTTTATAAAACAAATAATT"     , :qual "?????????????????????"     , :options []}
    {:qname "x4"  , :flag 0  , :rname "ref2", :pos 10, :mapq 30, :cigar "25M"               , :rnext "*", :pnext 0 , :tlen  0  , :seq "CAAATAATTAAGTCTACAGAGCAAC" , :qual "?????????????????????????" , :options []}
    {:qname "x6"  , :flag 0  , :rname "ref2", :pos 14, :mapq 30, :cigar "23M"               , :rnext "*", :pnext 0 , :tlen  0  , :seq "TAATTAAGTCTACAGAGCAACTA"   , :qual "???????????????????????"   , :options []}
    {:qname "x5"  , :flag 0  , :rname "ref2", :pos 12, :mapq 30, :cigar "24M"               , :rnext "*", :pnext 0 , :tlen  0  , :seq "AATAATTAAGTCTACAGAGCAACT"  , :qual "????????????????????????"  , :options []}]})

(defn get-shuffled-test-sam
  []
  (assoc test-sam :alignments (shuffle (:alignments test-sam))))

(def test-sam-only-header
  (assoc test-sam :alignments nil))

(def test-sam-sorted-by-pos
  {:header {:HD {:VN "1.4", :SO "coordinate"}
            :SQ [{:SN "ref", :LN 45} {:SN "ref2", :LN 40}]}
   :alignments
   [{:qname "r001", :flag 163, :rname "ref" , :pos 7 , :mapq 30, :cigar "8M4I4M1D3M"        , :rnext "=", :pnext 37, :tlen 39 , :seq "TTAGATAAAGAGGATACTG"       , :qual "*"                         , :options [{:XX {:type "B", :value "S,12561,2,20,112"}}]}
    {:qname "r002", :flag 0  , :rname "ref" , :pos 9 , :mapq 30, :cigar "1S2I6M1P1I1P1I4M2I", :rnext "*", :pnext 0 , :tlen 0  , :seq "AAAAGATAAGGGATAAA"         , :qual "*"                         , :options []}
    {:qname "r003", :flag 0  , :rname "ref" , :pos 9 , :mapq 30, :cigar "5H6M"              , :rnext "*", :pnext 0 , :tlen 0  , :seq "AGCTAA"                    , :qual "*"                         , :options []}
    {:qname "r004", :flag 0  , :rname "ref" , :pos 16, :mapq 30, :cigar "6M14N1I5M"         , :rnext "*", :pnext 0 , :tlen 0  , :seq "ATAGCTCTCAGC"              , :qual "*"                         , :options []}
    {:qname "r003", :flag 16 , :rname "ref" , :pos 29, :mapq 30, :cigar "6H5M"              , :rnext "*", :pnext 0 , :tlen 0  , :seq "TAGGC"                     , :qual "*"                         , :options []}
    {:qname "r001", :flag 83 , :rname "ref" , :pos 37, :mapq 30, :cigar "9M"                , :rnext "=", :pnext 7 , :tlen -39, :seq "CAGCGCCAT"                 , :qual "*"                         , :options []}
    {:qname "x1"  , :flag 0  , :rname "ref2", :pos 1 , :mapq 30, :cigar "20M"               , :rnext "*", :pnext 0 , :tlen 0  , :seq "AGGTTTTATAAAACAAATAA"      , :qual "????????????????????"      , :options []}
    {:qname "x2"  , :flag 0  , :rname "ref2", :pos 2 , :mapq 30, :cigar "21M"               , :rnext "*", :pnext 0 , :tlen 0  , :seq "GGTTTTATAAAACAAATAATT"     , :qual "?????????????????????"     , :options []}
    {:qname "x3"  , :flag 0  , :rname "ref2", :pos 6 , :mapq 30, :cigar "9M4I13M"           , :rnext "*", :pnext 0 , :tlen 0  , :seq "TTATAAAACAAATAATTAAGTCTACA", :qual "??????????????????????????", :options []}
    {:qname "x4"  , :flag 0  , :rname "ref2", :pos 10, :mapq 30, :cigar "25M"               , :rnext "*", :pnext 0 , :tlen 0  , :seq "CAAATAATTAAGTCTACAGAGCAAC" , :qual "?????????????????????????" , :options []}
    {:qname "x5"  , :flag 0  , :rname "ref2", :pos 12, :mapq 30, :cigar "24M"               , :rnext "*", :pnext 0 , :tlen 0  , :seq "AATAATTAAGTCTACAGAGCAACT"  , :qual "????????????????????????"  , :options []}
    {:qname "x6"  , :flag 0  , :rname "ref2", :pos 14, :mapq 30, :cigar "23M"               , :rnext "*", :pnext 0 , :tlen 0  , :seq "TAATTAAGTCTACAGAGCAACTA"   , :qual "???????????????????????"   , :options []}]})

(def test-sam-sorted-by-qname
  {:header {:HD {:VN "1.4", :SO "queryname"}
            :SQ [{:SN "ref", :LN 45} {:SN "ref2", :LN 40}]}
   :alignments
   [{:qname "r001", :flag 83 , :rname "ref" , :pos 37, :mapq 30, :cigar "9M"                , :rnext "=", :pnext 7 , :tlen -39, :seq "CAGCGCCAT"                 , :qual "*"                         , :options []}
    {:qname "r001", :flag 163, :rname "ref" , :pos 7 , :mapq 30, :cigar "8M4I4M1D3M"        , :rnext "=", :pnext 37, :tlen 39 , :seq "TTAGATAAAGAGGATACTG"       , :qual "*"                         , :options [{:XX {:type "B", :value "S,12561,2,20,112"}}]}
    {:qname "r002", :flag 0  , :rname "ref" , :pos 9 , :mapq 30, :cigar "1S2I6M1P1I1P1I4M2I", :rnext "*", :pnext 0 , :tlen 0  , :seq "AAAAGATAAGGGATAAA"         , :qual "*"                         , :options []}
    {:qname "r003", :flag 16 , :rname "ref" , :pos 29, :mapq 30, :cigar "6H5M"              , :rnext "*", :pnext 0 , :tlen 0  , :seq "TAGGC"                     , :qual "*"                         , :options []}
    {:qname "r003", :flag 0  , :rname "ref" , :pos 9 , :mapq 30, :cigar "5H6M"              , :rnext "*", :pnext 0 , :tlen 0  , :seq "AGCTAA"                    , :qual "*"                         , :options []}
    {:qname "r004", :flag 0  , :rname "ref" , :pos 16, :mapq 30, :cigar "6M14N1I5M"         , :rnext "*", :pnext 0 , :tlen 0  , :seq "ATAGCTCTCAGC"              , :qual "*"                         , :options []}
    {:qname "x1"  , :flag 0  , :rname "ref2", :pos 1 , :mapq 30, :cigar "20M"               , :rnext "*", :pnext 0 , :tlen 0  , :seq "AGGTTTTATAAAACAAATAA"      , :qual "????????????????????"      , :options []}
    {:qname "x2"  , :flag 0  , :rname "ref2", :pos 2 , :mapq 30, :cigar "21M"               , :rnext "*", :pnext 0 , :tlen 0  , :seq "GGTTTTATAAAACAAATAATT"     , :qual "?????????????????????"     , :options []}
    {:qname "x3"  , :flag 0  , :rname "ref2", :pos 6 , :mapq 30, :cigar "9M4I13M"           , :rnext "*", :pnext 0 , :tlen 0  , :seq "TTATAAAACAAATAATTAAGTCTACA", :qual "??????????????????????????", :options []}
    {:qname "x4"  , :flag 0  , :rname "ref2", :pos 10, :mapq 30, :cigar "25M"               , :rnext "*", :pnext 0 , :tlen 0  , :seq "CAAATAATTAAGTCTACAGAGCAAC" , :qual "?????????????????????????" , :options []}
    {:qname "x5"  , :flag 0  , :rname "ref2", :pos 12, :mapq 30, :cigar "24M"               , :rnext "*", :pnext 0 , :tlen 0  , :seq "AATAATTAAGTCTACAGAGCAACT"  , :qual "????????????????????????"  , :options []}
    {:qname "x6"  , :flag 0  , :rname "ref2", :pos 14, :mapq 30, :cigar "23M"               , :rnext "*", :pnext 0 , :tlen 0  , :seq "TAATTAAGTCTACAGAGCAACTA"   , :qual "???????????????????????"   , :options []}]})

(def test-sam-refs [{:name "ref", :len 45} {:name "ref2", :len 40}])

(def test-fa
  [{:rname "ref",  :offset 5,  :seq "AGCATGTTAGATAAGATAGCTGTGCTAGTAGGCAGTCAGCGCCAT", :blen 45}
   {:rname "ref2", :offset 57, :seq "aggttttataaaacaattaagtctacagagcaactacgcg",      :blen 40}])

(def sq-incomplete-alignments
  [{:SN "re",   :LN 45} ; not exists in alignments on test-sam
   {:SN "ref",  :LN 45}
   {:SN "ref1", :LN 40} ; not exists in alignments on test-sam
   {:SN "ref2", :LN 40}
   {:SN "ref3", :LN 45} ; not exists in alignments on test-sam
   ])
(def test-sam-incomplete-alignments
  {:header (assoc (:header test-sam) :SQ sq-incomplete-alignments)
   :alignments (:alignments test-sam)})
(def test-sam-incomplete-alignments-sorted-by-pos
  {:header (assoc (:header test-sam-sorted-by-pos) :SQ sq-incomplete-alignments)
   :alignments (:alignments test-sam-sorted-by-pos)})

(def temp-dir (.getPath (file (System/getProperty "java.io.tmpdir") "cljam-test")))

(defn prepare-cache!
  []
  (.mkdir (file temp-dir)))

(defn clean-cache!
  []
  (let [dir (file temp-dir)]
    (when (.exists dir)
      (doseq [f (seq (.list dir))]
        (.delete (file (str temp-dir "/" f))))
      (.delete dir))))


(defn- uniq [coll]
  (reduce
    (fn [r one]
      (if (= (first r) one)
        r
        (conj r one)))
    nil
    coll))

(defn- get-rnames [sam]
  (uniq (map :rname (:alignments sam))))

(defn check-sort-order [target-sam & [contrast-sam]]
  ;; TODO: only coordinate currently. need to test by queryname sort.
  (let [target-rnames (get-rnames target-sam)]
    ;; check rname groups
    (when contrast-sam
      (when-not (= target-rnames (get-rnames contrast-sam))
        (throw (Exception. "not matched by rnames order"))))
    ;; check order
    (dorun
      (map
        (fn [rname]
          (reduce
            (fn [prev one]
              (case (compare (:pos prev) (:pos one))
                -1 true
                1 (throw (Exception. "pos not sorted"))
                (case (compare (:qname prev) (:qname one))
                  -1 true
                  1 (throw (Exception. "qname not sorted"))
                  true))
              one)
            (filter #(= rname (:rname %)) (:alignments target-sam))))
        target-rnames))))

(def medium-bam-file "test/resources/medium.bam")
;;; How to generate "medium.bam":
;;; $ wget https://share.xcoo.jp/works/.../B6_all_bwa.sorted.bam
;;; $ samtools view -h B6_all_bwa.sorted.bam > src.sam
;;; $ grep '^@' src.sam > r.head
;;; $ grep -v '^@' src.sam > r.body
;;; $ cat r.body | perl -ne 'print $_ if rand() < 0.001' > r.body2
;;; $ cat r.head r.body2 > result.sam
;;; $ samtools view -S -b result.sam > test/resources/medium.bam

(def large-bam-file (cavy/resource "large.bam"))

