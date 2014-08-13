# lehs

## Lightweight Embeddable HTTP Server

lehs is a hyper-text transfer protocol (http) web server implemented in clojure.  Rather than being a full-feature, scalable web service, it is meant to provide a light-weight and easy to use API for embedding a web server (and, consequently, a web interface) into your application.

## How to start a lehs web service

Starting lehs is very easy.  You only need to call one function: `start-server`

``` clojure
; Start lehs, listening for connections on port 8080 (http) and 9999 (https)
(lehs.core/start-server 8080 9999)
```

Lehs will run indefinitely, handling user-agent requests and sending responses.

To stop the server, use the function `stop-server`:

```clojure
; stop the running lehs server
(stop-server)
```

You probably dont want to have your entire application hang on the web server, though, so start it as a future:
```clojure
; Start lehs as a background task
(def lehs-result (future (lehs.core/start-server 8080 9999)))

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
headers | Any headers that the user agent sent with the request.  This is also a map, e.g. `{:Accept "text/html"}`
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

### Adding directory contents

To create resources from the contents of a directory (recursively), use the function `defresource-dir.  If the contents of the directory ./data are the files

- ./data/foo.htm
- ./data/img/foo.png
- ./data/css/foo.css

The result of the following would be to add a resource for each, as "/data/foo.htm", "/data/img/foo.png" and /data/css/foo.css, respectively.  Each resource is created as a function the ignores the two arugments it is passed and returns a byte array of the file's contents.

```clojure
; add contents of data directory to resources
(defresource-dir ".\\data\\")
```

### Returning a response (map) structure

You may also return a map structure.  The defresource macro provides
your resource function with the variable `res`.  This variable
contains the preliminary response that lehs will send out.

Returning a map structure gives you fine-tuned control over the response that
lehs sends back to the user agent.  Using the variable `res` as a
base, you can associate into the map structure additional response
headers, the response code, etc.  For example, the following snippet sets
the location to include the fragment 'foo' and sets the response code
to 201.  It also returns in the message part a small web-page:

```clojure
(defresource "/d"
   (assoc-in-many res [[[:headers :Location] "/d#foo"]
                       [[:res-ln :code] 201]
                       [[:message] "<html><body><p id=\"foo\">Foo</p></body></html>"]]))
```

Keep in mind that you don't need to calculate the message length or do
any encoding; lehs does that for you automatically (except for
properly escaping any HTML text you dont want being rendered).

See appendix for full response map format and a description of the function `assoc-in-many`.

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

## lehs.common

- `assoc-in-many` Takes as arguments a map and a vector of map-paths to associate to new values.  For example:
```clojure
; define a simple map
(def m {:a 1 :b {:c 2}})
; set some paths using assoc-in-many
(assoc-in-many m [[[:b :d] 4] [[:a] 9]])
; result is {:a 9 :b {:c 2 :d 4}}
```
