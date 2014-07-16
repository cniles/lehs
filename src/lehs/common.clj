(ns lehs.common)

(defn split-at-subseq [s p]
  (let [c (count p) ps (seq p)]
    (loop [h [] s (lazy-seq s)]
      (cond (or (= ps (take-last c h)) (empty? s)) [h s]
        :else (recur (conj h (first s)) (drop 1 s))))))
