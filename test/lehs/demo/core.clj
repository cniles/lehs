(ns lehs.demo.core
  (:use hiccup.core
        hiccup.page
        lehs.core
        lehs.resource
        lehs.common))

(defresource-dir "./data/")

(defn make-sidebar []
  (html 
   [:div {:class "sidebar"}
    [:ul
     [:li [:a {:href "a"} "To page A"]]
     [:li [:a {:href "b"} "To page B"]]
     [:li [:a {:href "d"} "Bulletin board"]]
     [:li [:a {:href "/"} "Home"]]
     [:br]
     [:li [:a {:href "killserver"} "Kill lehs"]]]]
   [:div {:class "sidebar-fill"}]))

(defn make-header [title]
  (html [:div {:class "header"} [:h1 {:class "padded"} title]]
        [:div {:class "corner"}]))

(defn make-basic-page [title content]
  (.getBytes (html5 [:html
                    [:head (include-css "/data/style.css")]
                    [:body {:class "core"}
                     (make-header title)
                     [:div {:class "main"}
                      [:div {:class "inner"} content]]
		      (make-sidebar)]])))


(defresource "/" 
  (make-basic-page "Lehs Demo"
		   (html [:p "This website is only a series of pages used to test the lehs webserver."]
			 [:img {:src "/data/smiley.gif" :width "42" :height "42"}]
			 [:p "Thanks for visiting!"])))
	     

(defresource "/a"
  (make-basic-page "Page A"
		   (html [:p "This is page A"])))

(defresource "/b"
  (make-basic-page "Page B"
		   (html [:p "This is page B"])))

(defresource "/c"
  (make-basic-page "Bulletin Board"
		   (html [:br]
			 [:form {:action "d" :method "POST"}
                         "Name: " [:input {:type "text" :name "Name" :value "anon"}] [:br]
                         "Content: " [:input {:type "text" :name "Content" :value ""}] [:br]
                         [:input {:type "submit" :value "Submit"}]])))


; killserver page
(defresource "/killserver"
  (do (kill-server)
      (make-basic-page "Kill Lehs"
		       (html [:p "The Lehs webserver is shutting down"]))))

(defn -main [& args]
  (System/setProperty "javax.net.ssl.keyStore" "mySrvKeystore")
  (System/setProperty "javax.net.ssl.keyStorePassword" "clojure")
  (run-server 8080 9999))
