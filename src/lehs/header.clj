(ns lehs.header
  (:import [java.util Calendar Locale TimeZone]))

;
; Methods for creating response header
;

(defn response-line [code]
  (let [codes {200 "200 OK"
               404 "404 Not found"}]
    (str "HTTP/1.1" " " (codes code) "\r\n")))

(defn date-header []
  (let [calendar (Calendar/getInstance)]
    (str (.getDisplayName calendar Calendar/DAY_OF_WEEK Calendar/SHORT Locale/US) ", "
         (.get calendar Calendar/DAY_OF_MONTH) " "
         (.getDisplayName calendar Calendar/MONTH Calendar/SHORT Locale/US) " "
         (.get calendar Calendar/YEAR) " "
         (.get calendar Calendar/HOUR_OF_DAY) ":"
         (.get calendar Calendar/MINUTE) ":"
         (.get calendar Calendar/SECOND) " "
         (.getDisplayName (.getTimeZone calendar) false (TimeZone/SHORT))
         "\r\n"
    )))

(defn content-length-header [content]
  (str "Content-Length: " (count content) "\r\n"))

(defn content-type-header [content-type]
  (str "Content-Type: " content-type "\r\n"))

(def blank-ln "\r\n")
