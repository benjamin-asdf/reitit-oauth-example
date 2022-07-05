(ns benjamin.oauth-example
  (:require
   #_[muuntaja.core :as m]
   [reitit.ring :as ring]
   [reitit.coercion.spec]
   [reitit.ring.coercion :as rrc]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.adapter.undertow :refer [run-undertow]]
   #_[ring.middleware.session.cookie :as cookie]
   [ring.middleware.logger :as logger]
   [clojure.data.json :as json]
   [clojure.java.shell :as sh]
   [ring.middleware.session :as session]
   [ring.middleware.defaults :as defaults :refer [wrap-defaults ]]
   [ring.middleware.oauth2]))

(def
  creds
  (->
   (sh/sh "gpg" "--decrypt" "google-web-client-secret.gpg")
   :out
   json/read-json))

(def url "http://localhost:8080/")

(defn oauth-config []
  {:google
   {:authorize-uri    (-> creds :web :auth_uri)
    :access-token-uri (-> creds :web :token_uri)
    :client-id        (-> creds :web :client_id)
    :client-secret    (-> creds :web :client_secret)
    :scopes           ["email"]
    :launch-uri       "/oauth2/google"
    :redirect-uri     "/oauth2/google/callback"
    :landing-uri      "/"}})

(def outh-respones (atom []))
(defn wrap-oauth2
  [handler]
  (ring.middleware.oauth2/wrap-oauth2 handler (oauth-config)))

(def outh2-middleware
  {:name ::outh2
   :description "Authenticate with third party"
   :wrap wrap-oauth2})

(def auth-routes
  "These routes get handled by [[ring.middleware.oauth2]]."
  ["/oauth2"
   ["/google" {:get (constantly  nil)}]
   ["/google/callback" {:get (constantly  nil)}]])

(def requests (atom []))
(def server (atom nil))

(defn handler [{session :session}]
  (let [counter (inc (:counter session 0))]
    (println "hurr handler")
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (str counter)
     :session {:counter counter}}))

(defn
  home
  [_req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "home"})

(def app
  (ring/ring-handler
   (ring/router
    [["/" home]
     ["/api" ["/ping" handler]]
     auth-routes]
    {:data
     {:middleware
      [[wrap-defaults (-> defaults/site-defaults (dissoc :session))]
       logger/wrap-with-logger
       rrc/coerce-request-middleware
       muuntaja/format-response-middleware
       rrc/coerce-response-middleware]}})
   (ring/create-default-handler)
   {:middleware
    [parameters/parameters-middleware
     [session/wrap-session (-> defaults/site-defaults :session (assoc-in [:cookie-attrs :same-site] :lax))]
     outh2-middleware]}))

(defn
  run
  []
  (when-let [s @server] (.stop s))
  (reset! server (run-undertow #'app {:port 8080})))

(comment
  (run))

(comment

  "http://localhost:8080/oauth2/google"
  "http://localhost:8080"
  "http://localhost:8080/api/ping"

  (app {:request-method :get
        :uri "/api/ping"
        :query-params {:x "1", :y "2"}})

  (app
   {:request-method :get
    :uri "/oauth2/google"
    :scheme "http"
    :headers {"host" "localhost"}})

  (app (assoc (-> @requests last) :ring.middleware.oauth2/state "ru3-OfguVNa8"))


  (-> @requests second :session)
  (app (->  @requests last))

  "http://localhost:8080/oauth2/google/callback"

  "https://accounts.google.com/o/oauth2/auth"


  (require '[clojure.tools.deps.alpha.repl :refer [add-libs]])
  (add-libs
   '{com.github.jpmonettas/flow-storm-dbg {:mvn/version "2.2.99"}
     com.github.jpmonettas/flow-storm-inst {:mvn/version "2.2.99"}

        hiccup/hiccup {:mvn/version "2.0.0-alpha2"}

     })

  (require '[flow-storm.api :as fs-api])
  (fs-api/local-connect)

  )
