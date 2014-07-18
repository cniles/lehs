(ns lehs.demo.core
  (:use hiccup.core
        hiccup.page
        lehs.core
        lehs.resource
        lehs.common
        lehs.db))

(defresource "/foo.css"
  (.getBytes (slurp "foo.css")))

(defresource "/air.png"
  (slurp-bytes "air.png"))

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
	(.getBytes (html5 [:html
	      [:body
	      [:table
	      (map bb-entry-to-table-row bb)]
	      [:a {:href "/c"} "Add new message"] [:br]
	      [:a {:href "/d"} "Refresh"]
	      ]])))))

(defn -main [& args]
  (run-server 8080))
