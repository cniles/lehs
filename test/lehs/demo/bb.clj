(ns lehs.demo.bb
  (:use hiccup.core
        hiccup.page
        lehs.core
        lehs.resource
        lehs.common
        lehs.db))

(defn set-class-alt [[tag & vals]]
  (apply vector (concat [tag {:class "alt"}] vals)))
  

(defn get-bb-html []
  (let [rows (map (fn [r n] (if (odd? n) (assoc-in r [1 :class] "alt") r))
                   (map bb-entry-to-table-row (get-bb)) (range))]
    (.getBytes (html5 [:html
                       [:head (include-css "/data/foo.css")]
                       [:body
                        [:a {:href "/c"} "Add new message"] "|" [:a {:href "/d"} "Refresh"] "|" [:a {:href "/"} "Home"] [:b]
                        [:table {:id "bb"}
                         [:tr [:th "Name"] [:th "Message"]]
                         rows]
                        [:a {:href "/c"} "Add new message"] "|" [:a {:href "/d"} "Refresh"] "|" [:a {:href "/"} "Home"] [:b]]]))))


(defresource "/d"
  (assoc (if (= method :post)
           (let [oid (add-bb-entry message)]
             (assoc-in-many res [[[:headers :Location] (str "/d#" (.toString oid))]
                                 [[:res-ln :code] 201]]))
           res)
    :message (get-bb-html)))
