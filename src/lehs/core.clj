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

(defn write-to-stream [s d]
  (.write s (if (string? d) (.getBytes d) d)))

(defn write-to-gzip-stream [s d]
  (let [gs (java.util.zip.GZIPOutputStream. s)]
    (do (write-to-stream gs d)
        (println "Wrote compressed!!")
        (.finish gs))))

(defn write-response-to-stream [stream res]
  (do (write-to-stream stream (str (-> res :res-ln :version) " "
                                   (-> res :res-ln :code) " "
                                   (-> res :res-ln :reason-phrase) "\r\n"))
      (doall (map (fn [[k v]] (write-to-stream stream (str (name k) ": " v "\r\n")))
                  (res :headers)))
      (write-to-stream stream "\r\n")
      (if (= "gzip" (get-in res [:headers :Content-Encoding]))
        (write-to-gzip-stream stream (:message res))
        (write-to-stream (:message res)))))

(defn accept-connection-and-send-response [server-socket]

  "Accepts a connection to the server socket (only argument) and sends
  a response as side effects.  Also sends the processed request and
  response to stdout.  Returns nil if a request is received to the
  resource 'killserver', indicating that the server is to die.  Blocks
  until a socket connection is accepted (per java.net.ServerSocket/accept)"

  (let [socket (.accept server-socket)]
    (try
      (println "Client connected from " (.toString (.getRemoteSocketAddress socket)) "\n")
      (let [req (extract-req (.getInputStream socket))
            response (get-response req)]
        (println (str "Sending response:\n" response))
        (write-response-to-stream (.getOutputStream socket) response)
        (if (= "/killserver" (-> req :req-ln :uri :path))
          :kill))
      (catch Exception e (do (.printStackTrace e) (println (str "Exception occured: " (.getMessage e)))))
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
