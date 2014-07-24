(ns lehs.core
  (:use lehs.request
        lehs.response)
  (:import [java.net ServerSocket Socket]))

(def kill-server? (ref false))

(defn kill-server []
  (dosync (ref-set kill-server? true)))

(defn write-to-stream [s d]
  (.write s (if (string? d) (.getBytes d) d)))

(defn write-to-gzip-stream [s d]
  (let [gs (java.util.zip.GZIPOutputStream. s)]
    (do (write-to-stream gs d)
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

(defn read-req-and-send-response [socket]

  "Accepts a connection to the server socket (only argument) and sends
  a response as side effects.  Also sends the processed request and
  response to stdout.  Returns nil if a request is received to the
  resource 'killserver', indicating that the server is to die.  Blocks
  until a socket connection is accepted (per java.net.ServerSocket/accept)"

  (try
    (let [req (extract-req (.getInputStream socket))
          response (get-response req)]
      (println (str "Sending response:\n" response))
      (write-response-to-stream (.getOutputStream socket) response))
    (catch Exception e (do (.printStackTrace e) (println "Exception occured: " (.getMessage e))))))

(defn accept-connection [server-socket]
  (let [socket (.accept server-socket)]
    (println "Client connected from " (.toString (.getRemoteSocketAddress socket)) "\n")
    (future (with-open [socket socket] (read-req-and-send-response socket)))))

(defn server-loop [server-socket]
  (loop []
    (try (accept-connection server-socket)
         (catch java.net.SocketTimeoutException e nil))
    (if @kill-server? nil (recur))))
  
(defn run-server [http-port]
  (with-open [server-socket (ServerSocket. http-port 1)]
    (println (str "Server started on port " http-port ", listening for connections...\n"))
    (.setSoTimeout server-socket 500)
    (server-loop server-socket)
    (println "Server shutting down")
    'clean-exit))
