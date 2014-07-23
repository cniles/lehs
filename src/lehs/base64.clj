(ns lehs.base64
  (:use lehs.common))

; maps base-10 number in [0, 63] to base-64 alphabet
(def -b64-b-to-a (zipmap (range)
  (concat (map char (range (int \A) (inc (int \Z))))
          (map char (range (int \a) (inc (int \z))))
          (map char (range (int \0) (inc (int \9))))
          [\+ \/])))

; maps base-64 alphabet to base-10 number in [0,63]
(def -b64-a-to-b
  (invert-map -b64-b-to-a))

(defn encode-base64 [bs]
  "Encodes a sequence of characters or integral values into base-64.
  Only the first 8 bits of each sequence item is encoded."
  (flatten (pad-seq 4 \=
                    (map #(-b64-b-to-a (bits-to-int %))
                         (partition 6 6 (repeat 0) (mapcat #(get-bits % 8) bs))))))

(defn decode-base64 [bs]
  "Decodes base-64 encoded text into a byte array"
  (map bits-to-int
       (partition 8 (mapcat #(get-bits (-b64-a-to-b %) 6)
                            (take-while #(not= \= %) bs)))))
