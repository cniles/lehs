(ns lehs.core
  (:use lehs.request
        lehs.response)
  (:import [java.net ServerSocket Socket]
           [javax.net.ssl SSLServerSocketFactory]))

(def kill-server? (ref false))

(defn- start-server []
  (dosync (ref-set kill-server? false)))

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
    (.setSoTimeout server-socket 500)
    (loop []
      (try (accept-connection server-socket)
           (catch java.net.SocketTimeoutException e nil))
      (if @kill-server? nil (recur))))
  
(defn- create-server-socket [port]
  (ServerSocket. port))

(defn- create-ssl-server-socket [port]
  (.createServerSocket (SSLServerSocketFactory/getDefault) port))

(defn run-server [http-port https-port]
  (with-open [http-server-socket (create-server-socket http-port)
              https-server-socket (create-ssl-server-socket https-port)]
    (println "HTTP Server started on port " http-port "(http), " https-port "(https)")
    (start-server)
    (doall (map deref
                [(future (server-loop http-server-socket))
                 (future (server-loop https-server-socket))]))
    (println "Server shutting down")
    'clean-exit))
