(ns hello-server.core
  (:import (java.util.zip GZIPInputStream
                          GZIPOutputStream)
           (java.io ByteArrayInputStream
                    ByteArrayOutputStream))
  (:require [org.httpkit.server :as s]
            [compojure.core :refer [routes POST GET ANY]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [hiccup.core :refer [html]]
            [garden.core :refer [css]]
            [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan go >! <!]]
            [clojure.string :as string]
            [yaml.core :as yaml])
  (:gen-class))

(defn gzip-middleware
  "gzip-middleware gzips, memoizes the response body, and add the appropriate
  Content-Encoding headers for a gzip response."
  [handler]
  (fn [req]
    (let [res (handler req)
          res-gzip-headers (assoc-in res [:headers "Content-Encoding"] "gzip")]
      (log/info "Running gzip middleware...")
      (update-in
       res-gzip-headers
       [:body]
       (memoize
        (fn [body]
          (with-open [outputStream (ByteArrayOutputStream.)
                      gzip (GZIPOutputStream. outputStream)]
            (let [bodyBytes (bytes (byte-array (map (comp byte int) body)))]
              (.write gzip bodyBytes)
              (.close gzip)
              ;; HTTP-kit supports InputStream response but not byte[]
              ;; Convert the OutputStream to a byte[] and read it into an InputStream
              ;; http-kit will support byte[] in 2.4.0 https://github.com/http-kit/http-kit/pull/379
              (with-open [inputStream (ByteArrayInputStream.
                                       (-> outputStream .toByteArray))]
                inputStream)))))))))

(defn response-logging-middleware
  "Logging middleware to log response status and headers."
  [handler]
  (fn [req]
    (let [{status :status
           headers :headers
           body :body
           :as res} (handler req)]
      (log/info "Running response logging middleware...")
      (log/info (assoc headers :status status))
      res)))

(defn request-logging-middleware
  "Logging middleware to log all incoming requests."
  [handler]
  (fn [{uri :uri
        method :request-method
        :as req}]
    (do (log/info "Running request logging middleware...")
        (log/info (with-out-str (clojure.pprint/pprint req)))
        (handler req))))

(def cache-url
  "cache-url takes in a target url to slurp and returns the string.
  The return value is memoized."
  (memoize (fn [url]
             (try (slurp url)
                  (catch Exception e
                    (log/error (-> e .getMessage)))))))

(defn app []
  (let [external-styles ["https://necolas.github.io/normalize.css/8.0.1/normalize.css"
                         "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"]
        external-js ["https://unpkg.com/react@16/umd/react.production.min.js"
                     "https://unpkg.com/react-dom@16/umd/react-dom.production.min.js"]
        styles (future (string/join (map #(html [:style %])
                                         (conj (pmap #(cache-url %) external-styles)
                                               (css [:body {:margin 0}])))))
        script-tags (future (string/join (pmap #(html [:script {:defer true} (cache-url %)]) external-js)))]
    (routes
     (GET "/" [:as req]
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (html [:html
                     [:head
                      [:script @script-tags]
                      [:style @styles]]
                     [:body
                      [:div
                       [:h1.header "hello world!!"]]
                      [:main
                       [:pre
                        (let [common (slurp "https://raw.githubusercontent.com/timfpark/fabrikate-cloud-native/master/config/prod.yaml")]
                          (yaml/generate-string
                           (yaml/parse-string common)
                           :dumper-options {:flow-style :block}))]]]])})
     (route/not-found
      (html [:html
             [:head]
             [:body [:h1 "PAGE NOT FOUND!"]]])))))

(defn create-server []
  (s/run-server (response-logging-middleware
                 (gzip-middleware
                  (request-logging-middleware (app)))) {:port 9999}))

(defn stop-server [server]
  (server :timeout 100))

;;;; MAIN

(do
  (defonce server (atom nil))
  (when (not (nil? @server))
    (log/info "Restarting server...")
    (stop-server @server))
  (reset! server (create-server))
  (try
    (let [out (slurp "http://localhost:9999")]
      (prn (count out)))
    (catch Exception e (log/fatal (.getMessage e)))))

(defn -main [& args]
  (do
    (log/info "Starting server on port 9999...")
    (let [server (create-server)]
      (log/info "Started server on port 9999")
      server)))

#_(let [common (slurp "https://raw.githubusercontent.com/timfpark/fabrikate-cloud-native/master/config/prod.yaml")
        reifyed (print-str (yaml/generate-string
                            (yaml/parse-string common)
                            :dumper-options {:flow-style :block}))]
    (println reifyed))


