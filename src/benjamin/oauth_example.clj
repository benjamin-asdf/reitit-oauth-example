(require '[muuntaja.core :as m])
(require '[reitit.ring :as ring])
(require '[reitit.coercion.spec])
(require '[reitit.ring.coercion :as rrc])
(require '[reitit.ring.middleware.muuntaja :as muuntaja])
(require '[reitit.ring.middleware.parameters :as parameters])
(require '[ring.adapter.undertow :refer [run-undertow]])

(def app
  (ring/ring-handler
    (ring/router
     [
      ["/api"
       ["/math" {:get {:parameters {:query {:x int?, :y int?}}
                       :responses  {200 {:body {:total int?}}}
                       :handler    (fn [{{{:keys [x y]} :query} :parameters}]
                                     {:status 200
                                      :body   {:total (+ x y)}})}}]]

      ["/"
       {:get {:responses  {200 {:body string?}}
              :handler
              (fn [& _]
                {:status 200
                 :body "Hello"})}}]]

      ;; router data affecting all routes
      {:data {:coercion   reitit.coercion.spec/coercion
              :muuntaja   m/instance
              :middleware [parameters/parameters-middleware
                           rrc/coerce-request-middleware
                           muuntaja/format-response-middleware
                           rrc/coerce-response-middleware]}})))



(def server (atom nil))

(defn run []
  (when-let [s @server]
    (.stop s))
  (reset! server (run-undertow #'app {:port 8080})))

(comment

  (run)

  (require '[clojure.tools.deps.alpha.repl :refer [add-libs]])

  (add-libs '{         luminus/ring-undertow-adapter {:mvn/version "1.2.6"}})


  (defn handler [req]
    {:status 200
     :body "Hello world"})


  )

(comment

(app {:request-method :get
      :uri "/api/math"
      :query-params {:x "1", :y "2"}})

(app {:request-method :get
      :uri "/page"
      :query-params {:x "1", :y "2"}})
  )
