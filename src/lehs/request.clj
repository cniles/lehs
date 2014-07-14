(ns lehs.request)

;
; Process the request line
;
(def method-map {"OPTIONS" :options
                 "GET" :get
                 "HEAD" :head
                 "POST" :post
                 "PUT" :put
                 "DELETE" :delete
                 "TRACE" :trace
                 "CONNECT" :connect})

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
  (let [sseq (repeatedly #(char (.read stream)))]
    (loop [head [] s sseq]
      (if (= [\return \newline \return \newline] (take-last 4 head))
        [(clojure.string/split (apply str head) #"(\r\n)+") s] (recur (conj head (first s)) (drop 1 s))))))

(defn process-req-ln [req-str]
  (let [p (fn [[rs uri v]] {:method (method-map rs) :uri (process-uri uri) :version v})] (p (clojure.string/split req-str #" "))))

(defn process-req [head]
  {:req-ln (process-req-ln (first head)) :headers (process-headers (rest head))})

(defn extract-message-body [req s]
  (let [l (get (req :headers) "Content-Length")]
    (if (nil? l) ""
    (apply str (take (Integer/parseInt l) s)))))
