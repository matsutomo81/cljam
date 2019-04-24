(ns cljam.algo.pileup
  "Functions to calculate pileup from the BAM."
  (:require [cljam.io.sam :as sam]
            [cljam.io.sam.util.cigar :as cigar]
            [cljam.io.sam.util.flag :as flag]
            [cljam.io.sam.util.quality :as qual]
            [cljam.io.sam.util.refs :as refs]
            [cljam.io.pileup :as plpio])
  (:import [cljam.io.protocols SAMAlignment]
           [cljam.io.pileup PileupBase LocusPile]))

(defn- seq-step
  [^long start ^long end ^long step-size alns]
  (letfn [(starts-before? [^long pos ^SAMAlignment aln]
            (<= (.pos aln) pos))
          (ends-after? [^long pos ^SAMAlignment aln]
            (<= pos (.end aln)))
          (step [^long s carried-over rest-alns]
            (lazy-seq
             (when (<= s end)
               (let [e (Math/min (+ s step-size) end)
                     [in out] (split-with #(starts-before? e %) rest-alns)
                     pile (into carried-over in)
                     carry (filterv #(ends-after? (inc e) %) pile)
                     out-pos (some-> out ^SAMAlignment (first) .pos)
                     next-s (if-not (seq carry) (or out-pos (inc end)) (inc e))]
                 (cons [s pile] (step next-s carry out))))))]
    (let [[x :as xs] (drop-while #(< (.end ^SAMAlignment %) start) alns)]
      (when x
        (step (max start (.pos ^SAMAlignment x)) [] xs)))))

(defn pileup-seq
  "Returns a lazy sequence that each element contains [position (alignments
  piled-up at the locus)]."
  [^long start ^long end alns]
  (seq-step start end 0 alns))

(defn- quals-at-ref
  [idx ^String qual]
  (let [empty-qual? (and (= (.length qual) 1)
                         (= (.charAt qual 0) \*))]
    (if empty-qual?
      (vec (repeat (count idx) 93)) ;; \~
      (mapv (fn [[_ x]]
              (if (number? x)
                (qual/fastq-char->phred-byte (.charAt qual x))
                93))
            idx))))

(defn- seqs-at-ref
  [idx ^String s]
  (mapv (fn [[op x xs]]
          (let [c (if (number? x) (.charAt s x) x)]
            (case op
              :m [c]
              :d [c xs]
              :i [c (subs s (first xs) (last xs))]))) idx))

(defn index-cigar
  "Align bases and base quality scores with the reference coordinate."
  [^SAMAlignment aln]
  (let [idx (cigar/to-index (.cigar aln))]
    (assoc aln
           :seqs-at-ref (seqs-at-ref idx (:seq aln))
           :quals-at-ref (quals-at-ref idx (.qual aln)))))

(defn basic-mpileup-pred
  "Basic predicate function for filtering alignments for mpileup."
  [^long min-mapq]
  (fn [^SAMAlignment aln]
    (let [flag (.flag aln)]
      (and flag
           (zero? (bit-and (flag/encoded #{:unmapped
                                           :filtered-out
                                           :duplicated
                                           :supplementary
                                           :secondary}) flag))
           (or (not (flag/multiple? flag)) (flag/properly-aligned? flag))
           (not-empty (.cigar aln))
           (<= min-mapq (.mapq aln))))))

(defn resolve-base
  "Find a piled-up base and an indel from an alignment."
  [^long ref-pos ^SAMAlignment aln]
  (let [relative-pos (- ref-pos (.pos aln))
        qual ((:quals-at-ref aln) relative-pos)
        [base indel] ((:seqs-at-ref aln) relative-pos)]
    (-> (PileupBase.
         (zero? relative-pos)
         (.mapq aln)
         base
         qual
         (flag/reversed? (.flag aln))
         (= ref-pos (.end aln))
         (when-not (number? indel) indel)
         (when (number? indel) indel)
         (.qname aln))
        (assoc :alignment aln))))

(defn- resolve-bases
  [[ref-pos alns]]
  [ref-pos (mapv (partial resolve-base ref-pos) alns)])

(defn ->locus-pile
  "Convert a pile into `cljam.io.pileup.LocusPile`."
  [chr [pos pile]]
  (when (seq pile)
    (LocusPile. chr pos pile)))

(defn filter-by-base-quality
  "Returns a predicate for filtering piled-up reads by base quality at its
  position."
  [min-base-quality]
  (fn [p]
    (->> #(<= min-base-quality (.qual ^PileupBase %))
         (partial filterv)
         (update p 1))))

(defn- merge-corrected-quals
  "Merge corrected quals with the uncorrected part."
  [^SAMAlignment aln ^long correct-start corrected-quals]
  (let [quals (:quals-at-ref aln)
        start (.pos aln)
        len   (count corrected-quals)]
    (if (< start correct-start)
      (-> quals
          (subvec 0 (- correct-start start))
          (into corrected-quals)
          (into (subvec quals (+ (- correct-start start) len))))
      (into corrected-quals (subvec quals len)))))

(defn- correct-pair-quals
  "Correct quals of a pair. Returns a map with corrected quals."
  [^SAMAlignment a1 ^SAMAlignment a2]
  (when (and (pos? (.pnext a1))
             (<= (max (.pos a1) (.pos a2)) (.end a1)))
    (let [tlen1 (.tlen a1)
          tlen2 (.tlen a2)
          quals1 (:quals-at-ref a1)
          quals2 (:quals-at-ref a2)
          seqs1  (:seqs-at-ref a1)
          seqs2  (:seqs-at-ref a2)
          correct-start (max (.pos a1) (.pos a2))
          new-quals (for [pos (range correct-start
                                     (inc (min (.end a1) (.end a2))))]
                      (let [relative-pos1 (- pos (.pos a1))
                            relative-pos2 (- pos (.pos a2))
                            q1 (quals1 relative-pos1)
                            q2 (quals2 relative-pos2)
                            [b1] (seqs1 relative-pos1)
                            [b2] (seqs2 relative-pos2)]
                        (if (= b1 b2)
                          [(min 200 (+ q1 q2)) 0]
                          (if (<= q2 q1)
                            [(int (* 0.8 q1)) 0]
                            [0 (int (* 0.8 q2))]))))
          [new1 new2] (apply map vector new-quals)
          new-quals1 (merge-corrected-quals a1 correct-start new1)
          new-quals2 (merge-corrected-quals a2 correct-start new2)]
      (if (flag/r1? (.flag a1))
        [new-quals1 new-quals2]
        [new-quals2 new-quals1]))))

(defn- make-corrected-quals-map
  "Make a map which has corrected quals of all overlapping pairs."
  [alns]
  (->> alns
       (group-by (fn [x] (.qname ^SAMAlignment x)))
       (into {} (keep
                 (fn [[qname xs]]
                   (when (<= 2 (count xs))
                     [qname (apply correct-pair-quals xs)]))))))

(defn- correct-quals-at-ref
  "Returns an alignment with corrected quals by looking up quals
  in the corrected quals map."
  [corrected-map ^SAMAlignment aln]
  (if-not (= (.rnext aln) "=")
    aln
    (if-let [quals-pair (corrected-map (.qname aln))]
      (let [new-quals (if (flag/r1? (.flag aln))
                        (first quals-pair)
                        (second quals-pair))]
        (assoc aln :quals-at-ref new-quals))
      aln)))

(defn pileup
  "Piles up alignments in given region and returns a lazy sequence of
  `cljam.io.pileup.LocusPile`s.

  The following options are available:
  - `min-base-quality` Minimum quality of called bases [13]
  - `min-map-quality` Minimum quality of alignments [0]
  - `ignore-overlaps?` Disable detecting overlapped bases of PE reads [false]
  - `chunk-size` Size of a chunk to pile up at once [5000]"
  ([sam-reader region]
   (pileup sam-reader region {}))
  ([sam-reader
    {:keys [chr start end] :or {start 1 end Integer/MAX_VALUE}}
    {:keys [min-base-quality min-map-quality ignore-overlaps? chunk-size]
     :or {min-base-quality 13 min-map-quality 0 ignore-overlaps? false
          chunk-size 5000}}]
   (when-let [len (:len (refs/ref-by-name (sam/read-refs sam-reader) chr))]
     (let [s (max 1 start)
           e (min len end)
           region {:chr chr :start s :end e}]
       (->> (sam/read-alignments sam-reader region)
            (sequence
             (comp
              (filter (basic-mpileup-pred min-map-quality))
              (map index-cigar)))
            (seq-step start end chunk-size)
            (sequence
             (comp
              (mapcat (fn [[pos alns]]
                        (->> (if ignore-overlaps?
                               alns
                               (keep (partial correct-quals-at-ref
                                              (make-corrected-quals-map alns)) alns))
                             (pileup-seq pos (min end (+ pos chunk-size))))))
              (map resolve-bases)
              (if (pos? min-base-quality)
                (map (filter-by-base-quality min-base-quality))
                identity)
              (keep (partial ->locus-pile chr)))))))))

(defn align-pileup-seqs
  "Align multiple piled-up seqs."
  [& xs]
  (if (<= (count xs) 1)
    (map (fn [{:keys [pos] :as m}] [pos [m]]) (first xs))
    (letfn [(step [xs]
              (lazy-seq
               (when (some seq xs)
                 (let [min-pos (apply min (keep (comp :pos first) xs))]
                   (cons [min-pos (mapv
                                   (fn [[{:keys [pos] :as m}]]
                                     (when (= pos min-pos) m)) xs)]
                         (step (mapv
                                (fn [[{:keys [pos]} :as ys]]
                                  (if (= pos min-pos) (next ys) ys)) xs)))))))]
      (step xs))))

(defn mpileup
  "Pile up alignments from multiple sources.

  The following `options` are available:
  - `min-base-quality` Minimum quality of called bases [13]
  - `min-map-quality` Minimum quality of alignments [0]
  - `ignore-overlaps?` Disable detecting overlapped bases of PE reads [false]"
  [region options & sam-readers]
  (apply align-pileup-seqs (map #(pileup % region options) sam-readers)))

(defn create-mpileup
  "Creates a mpileup file from the BAM file.

  The following `options` are available:
  - `min-base-quality` Minimum quality of called bases [13]
  - `min-map-quality` Minimum quality of alignments [0]
  - `ignore-overlaps?` Disable detecting overlapped bases of PE reads [false]"
  ([in-sam out-mplp]
   (create-mpileup in-sam nil out-mplp))
  ([in-sam in-ref out-mplp]
   (create-mpileup in-sam in-ref out-mplp nil))
  ([in-sam in-ref out-mplp region]
   (create-mpileup in-sam in-ref out-mplp region nil))
  ([in-sam in-ref out-mplp region options]
   (with-open [s (sam/reader in-sam)
               w (plpio/writer out-mplp in-ref)]
     (let [regs (if region
                  [region]
                  (map
                   (fn [{:keys [name len]}]
                     {:chr name :start 1 :end len})
                   (sam/read-refs s)))]
       (doseq [reg regs]
         (plpio/write-piles w (pileup s reg options)))))))
