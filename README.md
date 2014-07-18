lehs
====

Lightweight Embeddable HTTP Server


Appendix
========

Request map format:
-------------------

{:req-ln {:method :get|:post|:head|:delete}
          :uri {:path "val",
                :query {:k1 "val", :k2 "val"}
                :fragment "val"}
          :version "val"}
 :headers {:k1 "val", :k2 "val"}
 :messsage {:foo "bar", :boo "far"}}


Response map format:
--------------------

{:res-ln "version code [description]"
 :headers {:k1 "val",
           :k2 "val"}
 :message byte-array | "string"}
