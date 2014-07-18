(ns lehs.response
  (:use lehs.resource
        lehs.header))

(defn accept-gzip? [req]
  (if (contains? (req :headers) :Accept-Encoding)
    (let [es (map #(re-find #"(\*|\w+);q=(0(\.\d?\d?\d?)?|1(\.0?0?0?)?)" %)
                  (clojure.string/split (-> req :headers :Accept-Encoding) #", "))]
      (contains? (into {} (map (fn [[_ e q]] [(keyword e) q]) es)) :gzip))
    false))
  
(defn gen-response [rf req code]
  (let [msg (rf req)]
    {:res-ln (response-line code),
     :headers {:Date (http-date-string),
               :Content-Length (count msg),
               :Content-Type (get-type req),
               :Content-Encoding (if (accept-gzip? req) "gzip" "identity")},
     :message msg}))

(def method-fns
  {:get
   (fn [req]
       (if (resource-exists? req) (gen-response (get-resource req) req 200)
           (gen-response (@pages :404) req 404)))

   :500
   (fn [req]
     (gen-response ((get @pages :500) req) 500))
   }
  )

(defn get-response [req]
  ((get method-fns (-> req :req-ln :method) (method-fns :500)) req))
