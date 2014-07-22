(ns lehs.base64
  (:use lehs.common))

(def pows-of-2 (map #(apply * (repeat % 2)) (range)))

(def base64-map (zipmap (range)
  (concat (map char (range (int \A) (inc (int \Z))))
          (map char (range (int \a) (inc (int \z))))
          (map char (range (int \0) (inc (int \9))))
          [\+ \/])))

(defn get-bit [b i]
  (if (bit-test b i) 1 0))

(defn get-bits [b]
  (map #(get-bit (int b) %) (reverse (range 8))))

(defn bits-to-int [bs] (reduce + (map #(* % %2) bs
                                      (reverse (take (count bs) pows-of-2)))))

(defn encode-base64 [bs]
  (flatten (pad-seq 4 \=
                    (map #(base64-map (bits-to-int %))
                         (partition 6 6 (repeat 0) (mapcat get-bits bs))))))
