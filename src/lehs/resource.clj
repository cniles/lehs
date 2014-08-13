(ns lehs.resource
    (:use lehs.common
          clojure.java.io))

; In lehs, a web page (or resource, script, etc.) is simply a string
; (e.g. /blog) thats mapped to a string-returning function.  The
; function MUST take as a single argument a map the contains the
; contents of the client request.  Two special case resources (:404
; and :500 are defined (and can be overridden) and used by the server
; accordingly.

; A reference to the map for all the pages 
(def pages (ref {}))

; A reference to the map for content types
(def type-map (ref {}))

(def extension-type-map (ref {}))

(defn get-extension [s]
  (second (re-find #"\.(.+)$" s)))

(defn get-type [{{{path :path} :uri} :req-ln}]
  (let [t (get @type-map path :not-found)]
       (if (= t :not-found)
	   (extension-type-map (get-extension path))
	 t)))

(defn map-extensions-to-type [type x & xs]
  (dosync (alter extension-type-map assoc x type)
	  (alter extension-type-map into (zipmap xs (repeat type)))))

;
; Some common extension types
;
(defmacro defextmapping 
  ([t x & xs]
     (flatten ['map-extensions-to-type t x xs]))
  ([t x]
     (list 'map-extensions-to-type t x)))

(defextmapping "text/css" "css")
(defextmapping "text/html" "htm" "html" "" nil)
(defextmapping "image/jpeg" "jpeg" "jpg" "jpe")
(defextmapping "image/gif" "gif")
(defextmapping "image/png" "png")

(defn add-resource
  "Associates a new page or resource into the map referenced by pages.
  If specified, also maps the resource to a type"
  ([r p] (dosync
          (alter pages assoc r p)))
  ([r p t] (dosync
            (alter pages assoc r p)
            (alter type-map assoc r t))))

(defmacro defresource
  "Macro for defining a new page.  Takes a string defining the
  resource name and an expression that evaluates to a string and calls
  add-page on it after wrapping it in an anonymous function header.

  The macro provides the supplied expression, p, the following
  arguments that are deconstructed from the provided request
  argument: method, headers, message, path, query, fragment"

  ([r p]
     (list 'add-resource r (list 'fn [{{'method :method {:keys ['path 'query 'fragment]} :uri} :req-ln
                                   'headers :headers
                                   'message :message} 'res]
                             p)))
  ([r p t]
     (list 'add-resource r (list 'fn [{{'method :method {:keys ['path 'query 'fragment]} :uri} :req-ln
                                   'headers :headers
                                   'message :message} 'res]
                             p) t)))

(defn defresource-dir [dir]
     (if (.isDirectory (file dir))
       (let [fs (filter #(and (.isFile %) (.canRead %))
                        (file-seq (file dir)))]
         (println "Adding resource dir")
         (dorun (for [f fs]
           (add-resource (to-abs-unix-path (.getPath f)) (fn [_ _] (slurp-bytes f))))))))

(defn get-resource [{{{path :path} :uri} :req-ln}]
  (get @pages path (get @pages :404)))

(defn resource-exists? [{{{path :path} :uri} :req-ln}]
  (contains? @pages path))

; Page not found
(defresource :404
  (str "<!DOCTYPE html>\n"
       "<html>"
       "<body><h1>404 - Resource not found</h1>"
       "<p>The specified resource, " path ", could not be found</p></body></html>") "text/html")

; Unsupported operation or client error
(defresource :500
  (str "<!DOCTYPE html>\n"
       "<html>"
       "<body><h1>500 - Resource not found</h1>"
       "<p>Unsupported request</p></body></html>") "text/html")

