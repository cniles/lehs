(ns lehs.resource
    (:use hiccup.core
    	  hiccup.page))

; In lehs, a web page (or resource, script, etc.) is simply a string
; (e.g. /blog) thats mapped to a string-returning function.  The
; function MUST take as a single argument a map the contains the
; contents of the client request.  Two special case resources (:404
; and :500 are defined (and can be overridden) and used by the server
; accordingly.

; A reference to the map for all the pages 
(def pages (ref {}))
(def type-map (ref {}))

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
                                   'message :message}]
                             p)))
  ([r p t]
     (list 'add-resource r (list 'fn [{{'method :method {:keys ['path 'query 'fragment]} :uri} :req-ln
                                   'headers :headers
                                   'message :message}]
                             p) t)))

; Request this resource to kill the server
(defresource "/killserver"
  (html [:html [:body [:h1 "killing server"]]])
  "text/html")

; Page not found
(defresource :404
  (html5 [:html [:body 
    [:h1 "404 - Resource not found"]
    [:p "The specified resource, " path ", could not be found"]]])
  "text/html")

; Unsupported operation
(defresource :500
  (html5 [:html [:body [:h1 "500 - Unsupported operation: " method]]])
  "text/html")
