# lehs

## Lightweight Embeddable HTTP Server

lehs is a hyper-text transfer protocol (http) web server implemented in clojure.  Rather than being a full-feature, scalable web service, it is meant to provide a light-weight and easy to use API for embedding a web server (and, consequently, a web interface) into your application.

## How to start a lehs web service

Starting lehs is very easy.  You only need to call one function: `run-server`

``` clojure
; Start lehs, listening for connections on port 8080
(lehs.core/run-server 8080)
```

Lehs will run indefinitely, handling user-agent requests and sending responses.

You probably dont want to have your entire application hang on the web server, though, so run it as a future:
```clojure
; Start lehs as a background task
(def lehs-result (future (lehs.core/run-server 8080)))

; When it's finished, it will terminate with the symbol 'clean-exit:
(if (= 'clean-exit @lehs-result) (println "Lehs terminated correctly")
                                 (println "An error occurred"))
```

## Adding resources

### Doing it manually

Lehs stores all of the resources it handles in a referenced map structure.  The resource itself, internally, is simply a function that returns a string, a byte array or a response map structure (see appendix).  As arguments, the resource function must accept two maps: the first is the request map that the server received from the user agent, the second is the response (so far) that lehs has put together that will be sent back.

For example, to create a simple web page containing the text "Hello world!":
```clojure
(defn hello-page [req res] "<html><body>Hello world!</html></body")
```
Then, to add the resource:
```clojure
(lehs.resource/add-resource "/hello.htm" hello-page)
```

### The easy way

Lehs provides a macro that makes it really easy to generate a new resource: `defresource`.  The macro will generate your function header (deconstructing the request map for you) and automatically add the resource to lehs' resource map.

```clojure
; Create and add our Hello world page, this time using defresource:
(defresource "/hello.htm" "<html><body>Hello world!</html></body>")
```

The request map is deconstructed into the following variables for your resource function to use:

Argument | Description
-------- | -----------
method | The request method (get, post, head etc...)
path | The path to the resource from request line's URI
query | The query part of the uri, which is actually a map of key-value pairs.  The query ?a=1&b=2 is parsed into `{:a 1, :b 2}`
fragment | The fragment part of the uri
headers | Any headers that the user agent ent with the request.  This is also a map, e.g. `{:Accept "text/html"}`
message | The decoded message body.  In the case of a form message body sent with a post request (application/x-www-form-urlencoded), it is decoded into a map.  E.g. {:Name "Joe" :Occupation "Programmer" :Age "28"}

So, if you receive a request for the URI "/foo/greet.html?name=Joe", your resource function can use the values in the query to tailor the response:
```clojure
(defresource "/foo/greet.html"
  (html5 [:html [:body [:h1 "Greetings!"] [:p (if (nil? (query :name)) "Hello!")
                                            (str "Hello, " (query :name) "!")]]]))
```
Resulting in some HTML like the following:
```html
<html>
  <body>
    <h1>Greetings!</h1>
    <p>Hello, Joe!</p>
  </body>
</html>
```

TODO: document returning response map structure instead of string (or byte array).

# Appendix

## Request map format:

```clojure
{:req-ln {:method :get|:post|:head|:delete
          :uri {:path "val",
                :query {:k1 "val", :k2 "val"}
                :fragment "val"}
          :version "val"}
 :headers {:k1 "val", :k2 "val"}
 :messsage {:foo "bar", :boo "far"}}
```

## Response map format:

```
{:res-ln {:version "string" :code int  :reason-phrase "string"}
 :headers {:k1 "val",
           :k2 "val"}
 :message byte-array | "string"}
```

## Project TODOs:

- [ ] Decode percent-encoding in URI query
- [ ] Support adding directory contents as a resource, e.g. `(defresource-dir "/images/")` should add all the files in the directory images into the lehs resource map.
- [ ] More documentation!!!
