(ns lehs.test-request
  (:use clojure.test
        lehs.resource
        lehs.core
        lehs.request
        lehs.response
        lehs.common))

(deftest test-extract-req
  (is (= 2 (count (read-head (java.io.StringReader. "")))))
  (is (= "FOOBAR" (first (read-head (java.io.StringReader. "FOOBAR\r\n\r\n")))))
  (is (empty? (first (read-head (java.io.StringReader. ""))))))
