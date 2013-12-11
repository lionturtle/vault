(ns vault.data-test
  (:require
    [clojure.test :refer :all]
    [vault.data :refer :all])
  (:import
    java.io.ByteArrayInputStream))


(defn data-fixture
  "Builds a string representing a data blob from the given sequence of values."
  [& values]
  (apply str "#vault/data\n" (interpose "\n\n" values)))


(defn read-data-string
  "Reads data from bytes from a string."
  [string]
  (-> string
      (.getBytes blob-charset)
      ByteArrayInputStream.
      read-data))



;; SERIALIZATION

(deftest print-data-blob
  (is (= "#vault/data\n{:alpha \"foo\", :omega \"bar\"}"
         (print-data-str {:omega "bar" :alpha "foo"})))
  (is (= "#vault/data\n[:foo \\b baz]\n\n{:name \"Aaron\"}\n\n:frobnitz"
         (print-data-str [:foo \b 'baz] {:name "Aaron"} :frobnitz)))
  (testing "with metadata"
    (is (= "#vault/data\n^{:type :bytes}\n[{:size 100}]"
           (print-data-str ^{:type :bytes} [{:size 100}])))))



;; DESERIALIZATION

(deftest read-non-data-blob
  (let [content "foobarbaz not a data blob"
        result (read-data-string content)]
    (is (instance? java.io.InputStream result))
    (is (= content (slurp result)))))


(deftest read-data-blob
  (let [content (data-fixture "[:foo]")
        result (read-data-string content)]
    (is (= '([:foo]) result))))


(deftest read-custom-tag
  (let [content (data-fixture "{:foo #my/tag :bar}")
        input (ByteArrayInputStream. (.getBytes content blob-charset))
        result (read-data {'my/tag str} input)]
    (is (= '({:foo ":bar"}) result))))


(deftest read-primary-bytes
  (let [primary-content "[1 \\2 :three]"
        content (data-fixture primary-content)
        result (read-data-string content)]
    (is (= '([1 \2 :three]) result))
    (is (not (nil? (meta result))))
    (is (bytes= (.getBytes primary-content blob-charset) (primary-bytes result)))))


(deftest read-primary-bytes-with-extra-values
  (let [primary-content "#{:bar \"baz\"}"
        content (data-fixture primary-content ":frobble")
        result (read-data-string content)]
    (is (= '(#{:bar "baz"} :frobble) result))
    (is (not (nil? (meta result))))
    (is (bytes= (.getBytes primary-content blob-charset) (primary-bytes result)))))
