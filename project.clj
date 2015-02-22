(defproject heroku-releases-copy "0.1.0-SNAPSHOT"
  :description "Methods to assist and track releases on Heroku"
  :url "https://github.com/raymcdermott/heroku-releases-copy"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [http-kit "2.1.18"]
                 [cheshire "5.3.1"]
                 [environ "1.0.0"]
                 [com.novemberain/monger "2.0.1"]])
