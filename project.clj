(defproject lehs "0.0.1"
  :description "A light-weight embeddable http server written in clojure"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [lein-light-nrepl "0.0.6"]
                 [hiccup "1.0.5"]
                 [com.novemberain/monger "2.0.0"]]
   :repl-options {:nrepl-middleware [lighttable.nrepl.handler/lighttable-ops]}
  )
