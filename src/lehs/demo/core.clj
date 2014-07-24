(ns lehs.demo.core
  (:use hiccup.core
        hiccup.page
        lehs.core
        lehs.resource
        lehs.common))

(defresource-dir ".\\data\\")

(defresource "/"
  (.getBytes (html5 [:html
          [:head (include-css "/data/foo.css")]
          [:body
           [:h1 "Lehs test home"]
           [:p "This website is only a series of pages used to test the lehs webserver."]
           [:ul
            [:li [:a {:href "a"} "To page A"]]
            [:li [:a {:href "b"} "To page B"]]
            [:li [:a {:href "d"} "Bulletin board"]]
            [:li [:a {:href "killserver"} "Kill lehs"]]]
	   [:img {:src "/data/smiley.gif" :width "42" :height "42"}]
           [:p "Thanks for visiting!"]]])))

(defresource "/a"
  (.getBytes (html5 [:html
                     [:head (include-css "/data/foo.css")]
                     [:body
                      [:p "This is page A"]
                      [:ul
                       [:li [:a {:href "b"} "To page B"]]
                       [:li [:a {:href "/"} "Home"]]
                       ]]])))

(defresource "/b"
  (.getBytes (html5 [:html
                     [:head (include-css "/data/foo.css")]
                     [:body
                      [:p "This is page B"]
                      [:ul
                       [:li [:a {:href "a"} "To page A"]]
                       [:li [:a {:href "/"} "Home"]]
                       ]]])))

(defresource "/c"
  (.getBytes (html5 [:html
                     [:head (include-css "/data/foo.css")]
                     [:body
                      [:form {:action "d" :method "POST"}
                       "Name: " [:input {:type "text" :name "Name" :value "anon"}] [:br]
                       "Content: " [:input {:type "text" :name "Content" :value ""}] [:br]
                       [:input {:type "submit" :value "Submit"}]
                       ]]])))

; killserver page
(defresource "/killserver"
  (do (kill-server)
      (html5 [:html [:body [:h1 "killing server"]]])))

(defn -main [& args]
  (run-server 8080))
