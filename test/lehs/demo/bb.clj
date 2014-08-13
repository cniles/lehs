(ns lehs.demo.bb
  (:use hiccup.core
        hiccup.page
        lehs.core
        lehs.resource
        lehs.common
        lehs.db
        lehs.demo.core))

(defn get-bb-html []
  (let [rows (map (fn [r n] (if (odd? n) (assoc-in r [1 :class] "bbAlt") r))
                  (map bb-entry-to-table-row (get-bb)) (range))]
    (.getBytes (html5 [:html
                       [:head (include-css "/data/style.css")]
                       [:body {:class "core"}
                        (make-header "New Post")
                        [:div {:class "main"}
                         [:div {:class "inner"}
                          [:p [:a {:href "/c"} "Add a post"]]
                          [:table {:class "bb"}
                           [:tr {:id "bbHeader"} [:th {:class "bb"} "Name"] [:th {:class "bb"} "Message"]]
                           rows]]]
                        (make-sidebar)]]))))

(defresource "/d"
  (assoc (if (= method :post)
           (let [oid (add-bb-entry message)]
             (assoc-in-many res [[[:headers :Location] (str "/d#" (.toString oid))]
                                 [[:res-ln :code] 201]]))
           res)
    :message (get-bb-html)))
