(ns lehs.base64
  (:use lehs.common))

(def pows-of-2 (map #(apply * (repeat % 2)) (range)))

(def base64-map (zipmap (range)
  (concat (map char (range (int \A) (inc (int \Z))))
          (map char (range (int \a) (inc (int \z))))
          (map char (range (int \0) (inc (int \9))))
          [\+ \/])))

(defn encode-base64 [bs]
  (flatten (pad-seq 4 \=
                    (map #(base64-map (bits-to-int %))
                         (partition 6 6 (repeat 0) (mapcat #(get-bits % 8) bs))))))

(defn decode-base64 [bs]
  (map bits-to-int
       (partition 8 (mapcat #(get-bits ((invert-map base64-map) %) 6)
                            (take-while #(not= \= %) bs)))))
