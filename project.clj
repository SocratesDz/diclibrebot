(defproject diclibrebot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure     "1.8.0"]
                 [environ                 "1.1.0"]
                 [morse                   "0.2.4"]
                 [reaver                  "0.1.2"]
                 [compojure               "1.6.1"]
                 [ring/ring-jetty-adapter "1.6.3"]]
  :min-lein-version "2.0.0"
  :main ^:skip-aot diclibrebot.core
  :target-path "target/%s"
  :plugins [[lein-environ "1.1.0"]]
  :uberjar-name "diclibrebot-standalone.jar"
  :profiles {:uberjar {:aot :all}
             :production {:env {:production true}}})
