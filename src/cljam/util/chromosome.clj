(ns cljam.util.chromosome
  "Utilities for handling chromosome name."
  (:require [clojure.string :as cstr]))

(defn normalize-name
  [s]
  (-> s
      (cstr/replace #"[,\.]" "_")
      (cstr/replace #"[\"\']" "")))

(defn trim-chromosome-key
  "Removes chr prefix from chromosome name."
  [s]
  (cstr/replace s #"(?i)^chr" ""))

(defn- normalize-chromosome-prefix
  [s]
  (if-let [[_ base leftover] (re-matches #"(?i)chr([0-9]{1,2}|X|Y|M|MT|Un)(.*)" s)]
    (let [base* (condp re-matches base
                  #"\d+" (str (Integer/parseInt base))
                  #"(?i)Un" "Un"
                  (cstr/upper-case base))]
      (str "chr" (str base* (cstr/upper-case leftover))))
    s))

(defn- prepend-chromosome-prefix
  [s]
  (if (re-matches #"(?i)([0-9]{1,2}|X|Y|M|MT|Un).*" s)
    (str "chr" s)
    s))

(defn normalize-chromosome-key
  "Normalizes chromosome name."
  [s]
  (-> s
      normalize-name
      prepend-chromosome-prefix
      normalize-chromosome-prefix))
