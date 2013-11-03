(defproject vault "0.2.0"
  :description "Content-addressible datastore."
  :url "https://github.com/greglook/vault"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/tools.cli "0.2.4"]
                 [digest "1.4.3"]
                 [fipp "0.4.1"]]
  :main vault.tool)
