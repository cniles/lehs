(ns lehs.common)

(defn split-after-subseq [s p]
  (let [c (count p) ps (seq p)]
    (loop [h [] s (lazy-seq s)]
      (cond (or (= ps (take-last c h)) (empty? s)) [h s]
        :else (recur (conj h (first s)) (drop 1 s))))))

(defn stream-seq [s]
  "Returns a lazy sequence of the items read off of a stream"
      (map char (take-while pos? (repeatedly #(.read s)))))

(defn map-funcs [fs c]
  "Returns a sequence consisting of applying first item in fs to the
  first item in c, and the second item in fs to the second item in c.
  That is, fs needs to be a sequence of functions taking
  one-argument (i.e. [f1, f2, f3, ..., fn]).  The returned result is the
  sequence [(f1 c1) (f2 c2) (f3 c3) ... (fn cn)]."
  (map #(% %2) fs c))

(defn get-key-value
  "Splits a string at its delimeter, and returns a tuple of [(keyword k) v]"
  ([s d]
      (get-key-value s d identity))
  ([s d f]
      (map-funcs [(comp keyword f) f]
		 (rest (re-find (re-pattern (str "(.+)" d "(.*)")) s)))))

(defn to-2nd [f [k v]]
  [k (f v)])

(def -urange ^{:private true}
  (apply vector (map byte (concat (range 0 128) (range -128 0)))))
(defn ubyte [n] (-urange (bit-and 0xff n)))

(defn slurp-bytes [fname]
  (byte-array (with-open [s (java.io.FileInputStream. fname)]
       (doall (map ubyte (take-while #(not= -1 %) (repeatedly #(.read s))))))))

(defn assoc-in-many [m ksvs]
  (reduce (fn [m [ks v]] (assoc-in m ks v)) m ksvs))

(defn to-abs-unix-path [path]
  "converts an operating system relative path (starting with .) to a unix-like absolute path" 
  (apply str (rest (clojure.string/replace path (java.io.File/separator) "/"))))

(defn pad-seq [m p s]
  (apply concat (partition m m (repeat m p) s)))

(defn invert-map [m]
  (into {} (for [[k v] m] [v k])))

(defn get-bit [b i]
  (if (bit-test b i) 1 0))

(defn get-bits [b n]
  (map #(get-bit (int b) %) (reverse (range n))))

(def -pows-of-2 ^{:private true}
  (iterate #(* 2 %) 1))
(defn bits-to-int [bs]
  (reduce + (map #(* % %2) bs
                 (reverse (take (count bs) -pows-of-2)))))
