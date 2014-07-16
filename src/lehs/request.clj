(ns lehs.request
  (:use lehs.common))

(defn method-keyword [s]
  (keyword (.toLowerCase s)))

(defn process-query [query-str]
  (apply hash-map (let [s (clojure.string/split query-str #"[=&\;]")]
                    (if (odd? (count s)) (butlast s) s))))

(defn process-uri [uri]
  (let [m (re-find #"^([^?#]*)\??([^#]*)?#?(.*)?" uri)]
    {:path (m 1)
     :query (process-query (m 2))
     :fragment (m 3)}))

(defn process-header [h]
  (rest (re-find #"([\w-]+): (.*)" h)))

(defn process-headers [c]
  (apply hash-map (mapcat process-header c)))

(defn read-head [stream]
  "Takes a stream and reads the request head (request line and headers, up to the first blank line.)  Returns a vector
  where the first item is the string value of the head and the second item is a lazy sequence of the message body."
  (let [sseq (repeatedly #(char (.read stream)))]
    ((fn [[h s]] [(clojure.string/split (apply str h) #"(\r\n)+") s]) (split-at-subseq sseq "\r\n\r\n"))))

(defn process-req-ln [req-str]
  (let [p (fn [[rs uri v]] {:method (method-keyword rs) :uri (process-uri uri) :version v})] (p (clojure.string/split req-str #" "))))

(defn process-req [head]
  {:req-ln (process-req-ln (first head)) :headers (process-headers (rest head))})


