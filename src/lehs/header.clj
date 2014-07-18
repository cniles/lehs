(ns lehs.header
  (:import [java.util Calendar Locale TimeZone]))

;
; Methods for creating response header
;

(defn response-line [code]
  (let [code-phrases {200 "OK"
                      404 "Not found"}]
    {:version "HTTP/1.1" :code code :reason-phrase (code-phrases code)}))

(defn http-date-string []
  (let [calendar (Calendar/getInstance)]
    (str (.getDisplayName calendar Calendar/DAY_OF_WEEK Calendar/SHORT Locale/US) ", "
         (.get calendar Calendar/DAY_OF_MONTH) " "
         (.getDisplayName calendar Calendar/MONTH Calendar/SHORT Locale/US) " "
         (.get calendar Calendar/YEAR) " "
         (.get calendar Calendar/HOUR_OF_DAY) ":"
         (.get calendar Calendar/MINUTE) ":"
         (.get calendar Calendar/SECOND) " "
         (.getDisplayName (.getTimeZone calendar) false (TimeZone/SHORT))
    )))

(def blank-ln "\r\n")
