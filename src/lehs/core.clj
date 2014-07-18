(ns lehs.core
  (:use hiccup.core
  	hiccup.page
        lehs.header
        lehs.request
        lehs.response
        lehs.decode
	lehs.common
        lehs.db)
  (:import [java.net ServerSocket Socket]))


(defn write-string-to-stream [stream s]
  (.write stream (.getBytes s)))

(defn write-response-to-stream [stream res]
  (do (write-string-to-stream stream (str (res :res-ln) "\r\n"))
      (doall (map (fn [[k v]] (write-string-to-stream stream (str (name k) ": " v "\r\n"))) (res :headers )))
      (write-string-to-stream stream "\r\n")
      (.write stream (res :message))))

(defn extract-req [stream]
  "Extracts the request from the an input stream"
  (let [head-and-body (read-head stream)
        head (process-req (head-and-body 0))]
    (assoc head :message (decode-message head (head-and-body 1)))))

(defn accept-connection-and-send-response [server-socket]

  "Accepts a connection to the server socket (only argument) and sends
  a response as side effects.  Also sends the processed request and
  response to stdout.  Returns nil if a request is received to the
  resource 'killserver', indicating that the server is to die.  Blocks
  until a socket connection is accepted
  (per java.net.ServerSocket/accept)"

  (let [socket (.accept server-socket)]
    (try
      (let [req (extract-req (.getInputStream socket))
            response (get-response req)]
        (println (str "Receieved request:\n " req "\n"))
        (println (str "Sending response:\n" response))
        (write-response-to-stream (.getOutputStream socket) response)
        (if (= "/killserver" (-> req :req-ln :uri :path))
          :kill))
      (catch Exception e (do (println (str "Exception occured: " (.getMessage e)))) :kill)
      (finally (.close socket)))))

(defn run-server [port]
  (let [server-socket (ServerSocket. port 1)]
    (println (str "Server started on port " port ", listening for connections...\n"))
    (loop [i 0]
      (if (accept-connection-and-send-response server-socket)
        nil
        (recur (inc i))))
      (.close server-socket))
    (println "Server shutting down")
    'clean-exit)

(defn -main
      "Run the server"
      [& args]
      (println "Hello world"))
