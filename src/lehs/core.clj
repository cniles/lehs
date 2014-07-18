(ns lehs.core
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:use hiccup.core
  	hiccup.page
	gzip-util.core
        lehs.header
        lehs.request
        lehs.decode
	lehs.resource
	lehs.common)
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
  (.getBytes (slurp "foo.css")))

(defresource "/air.png"
  (byte-array (let [s (java.io.FileInputStream. "air.png")]
       (map ubyte (take-while #(not= -1 %) (repeatedly #(.read s)))))))

(defresource "/"
  (.getBytes (html5 [:html
          [:head (include-css "foo.css")]
          [:body
           [:h1 "Lehs test home"]
           [:p "This website is only a series of pages used to test the lehs webserver."]
           [:ul
            [:li [:a {:href "a"} "To page A"]]
            [:li [:a {:href "b"} "To page B"]]
            [:li [:a {:href "d"} "Bulletin board"]]
            [:li [:a {:href "killserver"} "Kill lehs"]]]
	   [:img {:src "/air.png" :width "42" :height "42"}]
           [:p "Thanks for visiting!"]]])))

(defresource "/a"
  (.getBytes (html5 [:html
	[:body
	[:p "This is page A"]
	[:ul
	[:li [:a {:href "b"} "To page B"]]
	[:li [:a {:href "/"} "Home"]]
	]]])))

(defresource "/b"
  (.getBytes (html5 [:html
	[:body
	[:p "This is page B"]
	[:ul
	[:li [:a {:href "a"} "To page A"]]
	[:li [:a {:href "/"} "Home"]]
	]]])))

(defresource "/c"
  (.getBytes (html5 [:html
	[:body
	[:form {:action "d" :method "POST"}
	"Name: " [:input {:type "text" :name "Name" :value "anon"}] [:br]
	"Content: " [:input {:type "text" :name "Content" :value ""}] [:br]
	[:input {:type "submit" :value "Submit"}]
	]]])))

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
	      ]]))))

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

(defn get-resource [{{{path :path} :uri} :req-ln}]
  (get @pages path (get @pages :404)))

(defn resource-exists? [{{{path :path} :uri} :req-ln}]
  (contains? @pages path))

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

(defn write-string-to-stream [stream s]
  (.write stream (.getBytes s)))

(defn write-response-to-stream [stream res]
  (do (write-string-to-stream stream (str (res :res-ln) "\r\n"))
      (doall (map (fn [[k v]] (write-string-to-stream stream (str (name k) ": " v "\r\n"))) (res :headers )))
      (write-string-to-stream stream "\r\n")
      (.write stream (res :message))))

  

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
        (println (str "Receieved request:\n " req "\n"))
        (println (str "Sending response:\n" response))
        (write-response-to-stream (.getOutputStream socket) response)
        (if (= "/killserver" (-> req :req-ln :uri :path))
          :kill))
      (catch Exception e (do (println (str "Exception occured: " (.getMessage e)))) :kill)
      (finally (.close socket)))))

(defn run-server [port]
  (let [server-socket (ServerSocket. port 1)]
    (println (str "Server started on port " port ", listening for connections...\n"))
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
