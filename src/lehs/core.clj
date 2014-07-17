(ns lehs.core
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:use hiccup.core
  	hiccup.page
        lehs.header
        lehs.request
        lehs.decode
	lehs.resource)
  (:import [java.net ServerSocket Socket]))

;
; Database stuff...
;

(def conn (mg/connect))
(def db (mg/get-db conn "mydb"))
(defn add-bb-entry [entry] (if (every? entry [:Name :Content])
                             (mc/insert db "bb" (select-keys entry [:Name :Content]))))
(defn get-bb [] (mc/find-maps db "bb"))

; request map structure:
;{:req-ln {:method :get|:post|:...
;          :uri {:path "/p/a/t/h"
;                :query {"a" "1", "b" "2"}
;                :fragment "Frag"}
;          :version "HTTP/1.1"}
; :headers {"H1:" "1", "H2:" "2"}
; :messsage {:foo "bar", :boo "far"}


(defn bb-entry-to-table-row [r] [:tr [:td (r :Name ":")] [:td (r :Content)]])

(defresource "/foo.css"
  (slurp "foo.css")
  "text/css")

(defresource "/"
  (html5 [:html
          [:head (include-css "foo.css")]
          [:body
           [:h1 "Lehs test home"]
           [:p "This website is only a series of pages used to test the lehs webserver."]
           [:ul
            [:li [:a {:href "a"} "To page A"]]
            [:li [:a {:href "b"} "To page B"]]
            [:li [:a {:href "d"} "Bulletin board"]]
            [:li [:a {:href "killserver"} "Kill lehs"]]]
           [:p "Thanks for visiting!"]]])
  "text/html")

(defresource "/a"
  (html5 [:html
	[:body
	[:p "This is page A"]
	[:ul
	[:li [:a {:href "b"} "To page B"]]
	[:li [:a {:href "/"} "Home"]]
	]]])
  "text/html")

(defresource "/b"
  (html5 [:html
	[:body
	[:p "This is page B"]
	[:ul
	[:li [:a {:href "a"} "To page A"]]
	[:li [:a {:href "/"} "Home"]]
	]]])
  "text/html")

(defresource "/c"
  (html5 [:html
	[:body
	[:form {:action "d" :method "POST"}
	"Name: " [:input {:type "text" :name "Name" :value "anon"}] [:br]
	"Content: " [:input {:type "text" :name "Content" :value ""}] [:br]
	[:input {:type "submit" :value "Submit"}]
	]]])
  "text/html")

(defresource "/d"
  (do
   (if (= :post method)
     (do (println (str "adding db entry" message)) (add-bb-entry message)))
   (let [bb (get-bb)]
	(html5 [:html
	      [:body
	      [:table
	      (map bb-entry-to-table-row bb)]
	      [:a {:href "/c"} "Add new message"] [:br]
	      [:a {:href "/d"} "Refresh"]
	      ]])))
  "text/html")

(defresource "/killserver"
  (html [:html [:body [:h1 "killing server"]]]))

(defresource :404
  (html [:html [:body 
	[:h1 "404 - Gadzooks!"]
	[:p "The specified resource, " path ", could not be found"]]]))

(defresource :500
  (html [:html [:body [:h1 "500 - Unsupported operation: " method]]]))

;
; Response generators
;

(defn gen-head-response [rf req code]
  (let [msg (rf req)]
    (str (response-line code)
         (date-header)
         (content-length-header msg)
         (content-type-header (type-map (-> req :uri :path)))
         blank-ln)))

(defn gen-response [rf req code]
  (let [msg (rf req)]
    (str (response-line code)
         (date-header)
         (content-length-header msg)
         (content-type-header (type-map (-> req :uri :path)))
         blank-ln
         msg)))

(defn get-resource [{{{path :path} :uri} :req-ln}]
  (get @pages path (get @pages :404)))

(defn resource-exists? [{{{path :path} :uri} :req-ln}]
  (contains? @pages path))

(def method-fns
  {:get
   (fn [req]
       (if (resource-exists? req) (gen-response (get-resource req) req 200)
           (gen-response (@pages :404) req 404)))

   :post
    (fn [req]
     (gen-response ((get @pages (-> req :req-ln :uri :path) (get @pages :404)) req)
                   (if (contains? @pages (-> req :req-ln :uri :path)) 200 404)))

   :head
    (fn [req]
     (gen-head-response ((get @pages (-> req :req-ln :uri :path) (get @pages :404)) req)
                   (if (contains? @pages (-> req :req-ln :uri :path)) 200 404)))

   :500
   (fn [req]
     (gen-response ((get @pages :500) req) 500))
   }
  )

(defn write-to-socket [socket s]
  "Writes string s to the output stream of socket."
  (.write (.getOutputStream socket) (.getBytes s)))

(defn extract-req [stream]
  "Extracts the request from the an input stream"
  (let [head-and-body (read-head stream)
        head (process-req (head-and-body 0))]
    (assoc head :message (decode-message head (head-and-body 1)))))

(defn accept-connection-and-send-response [server-socket]

  "Accepts a connection to the server socket (only argument) and sends
  a response as side effects.  Also sends the processed request and
  response to stdout.  Returns nil if a request is received to the
  resource 'killserver', indicating that the server is to die.  Blocks
  until a socket connection is accepted
  (per java.net.ServerSocket/accept)"

  (let [socket (.accept server-socket)]
    (try
      (let [req (extract-req (.getInputStream socket))
            f (get method-fns (-> req :req-ln :method) (method-fns :500))
            response (f req)]
        (println (str "Receieved request:\n " req))
        (println (str "Sending response:\n" response))
        (write-to-socket socket response)
        (if (= "/killserver" (-> req :req-ln :uri :path))
          :kill))
      (catch Exception e (println (.getMessage e)))
      (finally (.close socket)))))

(defn run-server [port]
  (let [server-socket (ServerSocket. port 1)]
    (println (str "Server started on port " port ", listening for connections..."))
    (loop [i 0]
      (if (accept-connection-and-send-response server-socket)
        nil
        (recur (inc i))))
      (.close server-socket))
    (println "Server shutting down")
    'clean-exit)

(defn -main
      "Run the server"
      [& args]
      (println "Hello world"))
