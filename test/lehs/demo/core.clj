(ns lehs.demo.core
  (:use hiccup.core
        hiccup.page
        lehs.core
        lehs.resource
        lehs.common))

(defresource-dir ".\\data\\")

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

(defn make-header []
  (html [:div {:class "header"} [:h1 {:class "padded"} "Lehs Demo"]]
        [:div {:class "corner"}]))


(defresource "/"
  (.getBytes (html5[:html
                    [:head (include-css "/data/style.css")]
                    [:body {:class "core"}
                     (make-header)
                     [:div {:class "main"}
                      [:div {:class "inner"}
                       [:p "This website is only a series of pages used to test the lehs webserver."]
                       [:img {:src "/data/smiley.gif" :width "42" :height "42"}]
                       [:p "Thanks for visiting!"]]]
                     (make-sidebar)]])))

(defresource "/a"
  (.getBytes (html5 [:html
                     [:head (include-css "/data/style.css")]
                     [:body {:class "core"}
                      (make-header)
                      [:div {:class "main"}
                       [:div {:class "inner"}
                       [:p "This is page A"]]]
                     (make-sidebar)]])))

(defresource "/b"
  (.getBytes (html5 [:html
                     [:head (include-css "/data/style.css")]
                     [:body {:class "core"}
                      (make-header)
                      [:div {:class "main"}
                       [:div {:class "inner"}
                        [:p "This is page B"]]]
                     (make-sidebar)]])))

(defresource "/c"
  (.getBytes (html5 [:html
                     [:head (include-css "/data/style.css")]
                     [:body {:class "core"}
                      (make-header)
                      [:div {:class "main"}
                       [:div {:class "inner"}
                        [:br]
                        [:form {:action "d" :method "POST"}
                         "Name: " [:input {:type "text" :name "Name" :value "anon"}] [:br]
                         "Content: " [:input {:type "text" :name "Content" :value ""}] [:br]
                         [:input {:type "submit" :value "Submit"}]]]]
                      (make-sidebar)]])))


; killserver page
(defresource "/killserver"
  (do (kill-server)
      (html5 [:html [:body [:h1 "killing server"]]])))

(defn -main [& args]
  (System/setProperty "javax.net.ssl.keyStore" "mySrvKeystore")
  (System/setProperty "javax.net.ssl.keyStorePassword" "clojure")
  (run-server 8080 9999))
