(ns lehs.decode)

;
; Decoding functions
;

(defn pct-to-char [a b]
             (char (+ (Integer/parseInt (str a b) 16))))

(defn decode-pct-encoded [s]
            (if (>= (count s) 3)
              (let [a (nth s 0)
                    b (nth s 1)
                    c (nth s 2)]
                (if (re-matches #"%[0-9A-Za-z][0-9A-Za-z]" (str a b c))
                  (cons (pct-to-char b c) (decode-pct-encoded (nthrest s 3)))
                  (cons a (decode-pct-encoded (rest s))))) s))

(defn break-key-value [s]
  (rest (re-find #"(.+)=(.*)" s)))

(mapcat (fn [[k v]] [(keyword k) v])
     (partition 2 (mapcat break-key-value (clojure.string/split "a=1&b=" #"[&]"))))

(defn decode-url-encoded [s]
  (into {} (map (fn [[k v]] [(keyword k) v]) (partition 2 (map #(apply str (decode-pct-encoded (replace {\+ \space} %)))
                       (mapcat break-key-value (clojure.string/split s #"[&]")))))))

(def decoder-map
  {"application/x-www-form-urlencoded" decode-url-encoded})
