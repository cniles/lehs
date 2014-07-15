(ns lehs.core
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:use hiccup.core
        lehs.header
        lehs.request
        lehs.decode)
  (:import [java.net ServerSocket Socket]
           [java.util Calendar Locale TimeZone]))

;
; Database stuff...
;

(def conn (mg/connect))
(def db (mg/get-db conn "mydb"))
(defn add-bb-entry [entry] (mc/insert db "bb" entry))
(defn get-bb [] (mc/find-maps db "bb"))


(defmacro defpage
  "Wrapper macro for creating the page-generating functions"
  [pg]
  (list 'fn [{{'method :method {:keys ['path 'query 'fragment]} :uri} :req-ln
        'headers :headers
        'message :message}]
    pg))


; request map structure:
;{:req-ln {:method :get|:post|:...
;          :uri {:path "/p/a/t/h"
;                :query {"a" "1", "b" "2"}
;                :fragment "Frag"}
;          :version "HTTP/1.1"}
; :headers {"H1:" "1", "H2:" "2"}
; :messsage {:foo "bar", :boo "far"}


(defn bb-entry-to-table-row [r] [:tr [:td (r :Name ":")] [:td (r :Content)]])

(def pages {"/"
            (defpage
              (html [:html
                  [:body
                   [:h1 "This is the root page"]
                   [:ul
                    [:li [:a {:href "a"} "To page A"]]
                    [:li [:a {:href "b"} "To page B"]]
                    [:li [:a {:href "d"} "Bulletin board"]]
                    [:li [:a {:href "killserver"}"Kill it"]]]
                   [:p "Thanks for visiting!"]
                   ]]))

            "/a"
            (defpage
              (html [:html
                     [:body
                      [:p "This is page A"]
                      [:ul
                       [:li [:a {:href "b"} "To page B"]]
                       [:li [:a {:href "/"} "Home"]]
                      ]]]))

            "/b"
            (defpage
              (html [:html
                     [:body
                      [:p "This is page B"]
                      [:ul
                       [:li [:a {:href "a"} "To page A"]]
                       [:li [:a {:href "/"} "Home"]]
                      ]]]))

            "/c"
            (defpage
              (html [:html
                     [:body
                      [:form {:action "d" :method "POST"}
                       "Name: " [:input {:type "text" :name "Name" :value "anon"}] [:br]
                       "Content: " [:input {:type "text" :name "Content" :value ""}] [:br]
                       [:input {:type "submit" :value "Submit"}]
                       ]]]))

            "/d"
            (defpage
              (do
                (if (= :post method)
                  (do (println (str "adding db entry" message)) (add-bb-entry message)))
                (let [bb (get-bb)]
                  (html [:html
                       [:body
                        [:table
                         (map bb-entry-to-table-row bb)]
                        ;[:table {:style "width:300px"}
                        ; '([:tr [:td "Hello"] [:td "1"]] [:tr [:td "world"] [:td 2]])]
                        [:a {:href "/c"} "Add new message"] [:br]
                        [:a {:href "/d"} "Refresh"]
                        ]]))))

            "/killserver"
            (fn [{q :query p :path f :fragment}] (html [:html [:body [:h1 "killing server"]]]))

            :404
            (defpage
              (html [:html [:body [:h1 "404 - Resource not found"] [:p "The specified resource, " path ", could not be found"]]]))

            :500
            (defpage
              (html [:html [:body [:h1 "500 - Unsupported operation: " method]]]))

            })


;
; Returns a response (string)
;
(defn gen-response [msg code]
  (str (response-line code)
       (date-header)
       (content-length-header msg)
       (content-type-header "text/html")
       blank-ln
       msg))

(def method-fns
  {:get
   (fn [req]
     (gen-response ((get pages (-> req :req-ln :uri :path) (get pages :404)) req)
                   (if (contains? pages (-> req :req-ln :uri :path)) 200 404)))

   :post
    (fn [req]
     (gen-response ((get pages (-> req :req-ln :uri :path) (get pages :404)) req)
                   (if (contains? pages (-> req :req-ln :uri :path)) 200 404)))

   :500
   (fn [req]
     (gen-response ((get pages :500) req) 500))
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

  "Accepts a connection to the server socket (only argument) and sends a response as side effects.  Also sends the processed request and response to stdout.
  Returns nil if a request is received to the resource 'killserver', indicating that the server is to die.  Blocks until a socket connection is accepted
  (per java.net.ServerSocket/accept)"

  (let [socket (.accept server-socket)]
    (try
      (let [req (extract-req (.getInputStream socket))
            f (get method-fns (-> req :req-ln :method) (method-fns :500))
            response (f req)]
        (println (str "Receieved request " req))
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


(def run (future (run-server 8080)))

;@run
