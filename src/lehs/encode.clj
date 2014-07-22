(def base64-map (zipmap (range) 
  (concat (map char (range (int \A) (inc (int \Z))))
          (map char (range (int \a) (inc (int \z)))))))

(defn get-bit [b i] (if (bit-test b i) 1 0))

(defn get-bits [b] (map #(get-bit b %) (reverse (range 8))))

(def pows-of-2 (map #(apply * (repeat % 2)) (range)))

(defn bits-to-int [bs] (reduce + (map #(* % %2) bs (reverse (take (count bs) pows-of-2)))))

(defn encode-base64 (concat 
  (partition 4 4 \= 
    (map #(base64-map (bits-to-int %))
      (partition 6 6 0 (mapcat get-bits [bs]))))))
