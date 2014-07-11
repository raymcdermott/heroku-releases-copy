(defproject heroku-releases-copy "0.1.0-SNAPSHOT"
  :description "Methods to assist and track releases on Heroku"
  :url "https://github.com/raymcdermott/heroku-releases-copy"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [http-kit "2.1.18"]
                 [cheshire "5.3.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [com.novemberain/monger "2.0.0-rc1"]])
