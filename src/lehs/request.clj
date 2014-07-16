(ns lehs.request
  (:use lehs.common))

(defn method-keyword [s]
  (keyword (.toLowerCase s)))

(defn process-query [query-str]
  (apply hash-map (let [s (clojure.string/split query-str #"[=&\;]")]
                    (if (odd? (count s)) (butlast s) s))))

(defn process-uri [uri]
  "Takes an an argument a URI string, and returns a map with the
   keys :path, :query, and :uri associated with those values of the
   URI"
  (let [m (re-find #"^([^?#]*)\??([^#]*)?#?(.*)?" uri)]
    {:path (m 1)
     :query (process-query (m 2))
     :fragment (m 3)}))

(defn process-header [h]
  "Takes an HTTP header string (e.g. \"Content-Length: 123\" and
  returns a two-tuple of a keyword created from the header name and
  its value (as a string)"
  ((fn [[_ k v]] [(keyword k) v]) (re-find #"([\w-]+): (.*)" h)))

(defn process-headers [c]
  (apply hash-map (mapcat process-header c)))

(defn read-head [stream]
  "Takes a stream and reads the request head (request line and
  headers, up to the first blank line.)  Returns a vector where the
  first item is the string value of the head and the second item is a
  lazy sequence of the message body."
  (let [s (stream-seq stream)]
    ((fn [[h t]] [(clojure.string/split (apply str h) #"(\r\n)+") t])
     (split-after-subseq s "\r\n\r\n"))))

(defn process-req-ln [req-str]
  (let [p (fn [[rs uri v]]
	      {:method (method-keyword rs)
	      :uri (process-uri uri)
	      :version v})]
       (p (clojure.string/split req-str #" "))))

(defn process-req [head]
  "Takes a sequence of strings where the first is the request line and
  the rest are the headers of an HTTP request.  Returns a map with the
  keys :req-ln and :headers, associated with those parts of the
  request."
  {:req-ln (process-req-ln (first head)) :headers (process-headers (rest head))})


