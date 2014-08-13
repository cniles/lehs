(ns lehs.response
  (:use lehs.resource
        lehs.common
        lehs.header))

(defn get-code [req] (if (resource-exists? req) 200 404))

(defn accept-gzip? [req]
  (if (contains? (req :headers) :Accept-Encoding)
    (let [es (map #(re-find #"(gzip|compress|deflate|identity)(;q=(0(\.\d\d\d)?|1(\.000)?))?" %)
                  (clojure.string/split (-> req :headers :Accept-Encoding) #", "))]
      (contains? (into {} (map (fn [[_ e q]] [(keyword e) q]) es)) :gzip))
    false))
  
(defn gen-base-response [req code]
  {:res-ln (response-line code),
   :headers {:Server "[lehs]",
             :Date (http-date-string),
             :Content-Type (get-type req),
             :Content-Encoding (if (accept-gzip? req) "gzip" "identity")}
   :message ""})

(defn gen-get-response [rf req code]
  (let [base-res (gen-base-response req code)
        msg (rf req base-res)]
    (if (map? msg)
      (assoc-in msg [:headers :Content-Length] (count (msg :message)))
      (assoc-in-many base-res
                     [[[:headers :Content-Length] (count msg)]
                      [[:message] msg]]))))

(defn gen-head-response [rf req code]
  (assoc (gen-get-response rf req code) :message ""))

(defn gen-post-response [rf req code]
  (let [base-res (gen-base-response req code)
        out (rf req  base-res)]
    (if (map? out) 
      (assoc-in-many out
                     [[[:res-ln :code] (if (zero? (count (out :message))) 204 200)]
                      [[:headers :Content-Length] (count (out :message))]])
      (assoc-in-many base-res
                     [[[:res-ln :code] (if (zero? (count out)) 204 200)]
                      [[:headers :Content-Length] (count out)]
                      [[:message] out]]))))

(def method-fns
  {:get
   (fn [req]
     (gen-get-response (get-resource req) req (get-code req)))

   :head
   (fn [req]
     (gen-head-response (get-resource req) req (get-code req)))

   :post
   (fn [req]
     (gen-post-response (get-resource req) req (get-code req)))

   :500
   (fn [req]
     (gen-get-response (get-resource :500) req 500))
   }
  )

(defn get-response [req]
  (println (str "Received request:\n" req "\n"))
  (if (resource-exists? req) ((get method-fns (-> req :req-ln :method) (method-fns :500)) req)
      (gen-get-response (get-resource :404) req 404)))
