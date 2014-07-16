(ns lehs.decode
    (:use lehs.common))

;
; Decoding functions
;

(defn pct-to-char [a b]
  "Takes two hex-chars, a and b, and returns the character their
  concatenation with radix 16"
  (char (+ (Integer/parseInt (str a b) 16))))

(defn decode-pct-encoded [s]
  "Decodes a percent-encoded string. E.g. \"Hello%20World\" -> \"Hello world\""
  (if (>= (count s) 3)
      (let [a (nth s 0)
	   b (nth s 1)
	   c (nth s 2)]
	   (if (re-matches #"%[0-9A-Za-z][0-9A-Za-z]" (str a b c))
	       (cons (pct-to-char b c) (decode-pct-encoded (nthrest s 3)))
	     (cons a (decode-pct-encoded (rest s))))) s))

;
; decodes the key-value pair encoding of a typical post request
;
(defn decode-url-encoded [s]
  (into {} (map (fn [[k v]] [(keyword k) v])
		(partition 2 (map #(apply str (decode-pct-encoded (replace {\+ \space} %)))
				  (mapcat #(get-key-value % "=")
					  (clojure.string/split s #"[&]")))))))

(def decoder-map
  {"application/x-www-form-urlencoded" decode-url-encoded
   nil (fn [s] "")})

(defn extract-message-body [req s]
  (let [l (-> req :headers :Content-Length)]
    (if (nil? l) ""
      (apply str (take (Integer/parseInt l) s)))))

(defn decode-message [head msg]
  "Decodes an http request message.  Takes as arguments head, a map
  structure (as describe in core.clj) sans the :message key, and msg,
  a lazy-sequence of characters extracted from the remainder of the
  request stream.  Its important to not read too many characters, as
  read will block until either the socket is closed or it retrieves
  another byte--and the client isn't required to close the connection
  when its done transmitting.  The appropriate number of bytes must be
  read from the stream, so the head must be extracted first so that
  the content-length (or otherwise) can be determined.

  This function decodes the msg depending on the value set to the
  'Content-Type' header value.  It looks up the function to be applied
  in decoder-map.  If an appropriate decoder is not found, an empty
  string is returned."
  ((-> head :headers :Content-Type decoder-map)
   (extract-message-body head msg)))
