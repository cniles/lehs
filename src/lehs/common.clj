(ns lehs.common)

(defn split-after-subseq [s p]
  (let [c (count p) ps (seq p)]
    (loop [h [] s (lazy-seq s)]
      (cond (or (= ps (take-last c h)) (empty? s)) [h s]
        :else (recur (conj h (first s)) (drop 1 s))))))

(defn stream-seq [s]
  "Returns a lazy sequence of the items read off of a stream"
      (repeatedly #(char (.read s))))

(defn map-funcs [fs c]
  "Returns a sequence consisting of applying first item in fs to the
  first item in c, and the second item in fs to the second item in c.
  That is, fs needs to be a sequence of functions taking
  one-argument (i.e. [f1, f2, f3, ..., fn]).  The returned result is the
  sequence [(f1 c1) (f2 c2) (f3 c3) ... (fn cn)]."
  (map #(% %2) fs c))

(defn get-key-value [s d]
  "Splits a string at its delimeter, and returns a tuple of [(keyword k) v]"
  (map-funcs [keyword identity]
	     (rest (re-find (re-pattern (str "(.+)" d "(.*)")) s))))