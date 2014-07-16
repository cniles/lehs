(ns lehs.common)

(defn split-after-subseq [s p]
  (let [c (count p) ps (seq p)]
    (loop [h [] s (lazy-seq s)]
      (cond (or (= ps (take-last c h)) (empty? s)) [h s]
        :else (recur (conj h (first s)) (drop 1 s))))))

(defn stream-seq [s]
      (repeatedly #(char (.read s))))