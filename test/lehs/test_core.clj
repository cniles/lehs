(ns lehs.test-core
  (:use clojure.test
        lehs.resource
        lehs.core
        lehs.request
        lehs.response
        lehs.common))

(defresource "/test"
  (.getBytes "<html><body></body></html>")
  "text/html")

(def test-write-output (str "HTTP/1.1 200 OK\r\n"
                            "Date: " (lehs.header/http-date-string) "\r\n"
                            "Content-Length: 26\r\n"
                            "Content-Type: text/html\r\n"
                            "Content-Encoding: gzip\r\n\r\n"
                            "<html><body></body></html>"))

(def test-req {:req-ln {:method :get,
                        :uri {:path "/test",
                              :query {:a "1", :b "2"}
                              :fragment "Foo"}
                        :version "HTTP/1.1"}
               :headers {:Accept-Encoding "gzip;q=1.0",
                         :Accept-Type "text/html",
                         :Content-Type "application/x-www-form-urlencoded",
                         :Content-Length "7"}
               :message {:a "1", :b "2"}})

(def test-res {:res-ln {:version "HTTP/1.1" :code 200 :reason-phrase "OK"},
               :headers {:Date (lehs.header/http-date-string),
                         :Content-Length 26,
                         :Content-Type "text/html",
                         :Content-Encoding "gzip"},
               :message (.getBytes "<html><body></body></html>")})


(defn fix-date [{res-ln :res-ln headers :headers msg :message}]
  {:res-ln res-ln :headers (assoc headers :Date "1234") :message msg}) 

(deftest test-extract-req
  (is (= (fix-date test-req)
         (fix-date (extract-req (java.io.StringReader.
                                 (str 
                                  "GET /test?a=1&b=2#Foo HTTP/1.1\r\n"
                                  "Accept-Encoding: gzip;q=1.0\r\n"
                                  "Accept-Type: text/html\r\n"
                                  "Content-Length: 7\r\n"
                                  "Content-Type: application/x-www-form-urlencoded"
                                  "\r\n\r\n"
                                  "a=1&b=2")))))))

(defn msg-to-seq [res] (assoc res :message (seq (res :message))))

(deftest test-gen-response
  (is (true? (accept-gzip? {:headers {:Accept-Encoding "gzip;q=1.0"}})))
  (is (= (fix-date (msg-to-seq test-res))
         (fix-date (msg-to-seq (gen-get-response (@pages (-> test-req :req-ln :uri :path))
                                   test-req
                                   200))))))


(deftest test-resource-fns
  (is (= nil (get-extension "/foo"))
      (= "png" (get-extension "/foo.jpg")))
)

;(deftest test-writes
;  (is (= test-write-output
;         (let [stream (java.io.ByteArrayOutputStream.)]
;           (do (write-response-to-stream stream test-res)
;               (.toString stream))))))

(deftest test-common
  ;; two-compliment -128 is 128 unsigned.
  (is (= 128)
         (ubyte (byte -128)))
  (is (= 1 (ubyte 1)))
  (is (= 127 (ubyte 127)))
  (is (= -128 (ubyte 128)))
  (is (every? #(= java.lang.Byte %) (map (comp type ubyte) (range 0 255)))))
