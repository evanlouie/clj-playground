(defproject hello-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/core.async "0.4.490"]
                 [http-kit "2.3.0"]
                 [hiccup "1.0.5"]
                 [garden "1.3.6"]
                 [compojure "1.6.1"]
                 [io.forward/yaml "1.0.9"]]
  :main ^:skip-aot hello-server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
