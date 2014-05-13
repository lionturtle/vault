(ns vault.data.signature-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [mvxcvi.crypto.pgp :as pgp]
    (puget
      [data]
      [printer :as puget])
    [vault.blob.core :as blob]
    [vault.blob.store.memory :refer [memory-store]]
    (vault.data
      [edn :as edn-data]
      [signature :as sig]))
  (:import
    ; FIXME: why is this necessary??
    ; clojure.lang.Compiler$HostExpr.tagToClass(Compiler.java:1060)
    (org.bouncycastle.openpgp
      PGPPrivateKey
      PGPSecretKey)))


(def blob-store (memory-store))

(def test-keyring
  (pgp/load-secret-keyring
    (io/file (io/resource "test-resources/pgp/secring.gpg"))))

(def pubkey
  (pgp/get-public-key test-keyring "923b1c1c4392318a"))

(def pubkey-id
  (->> pubkey
       pgp/encode-ascii
       (blob/store! blob-store)
       :id))


(deftest no-signature-blob
  (let [blob (-> {:foo 'bar}
                 (edn-data/edn-blob [{:baz 123}])
                 (sig/verify blob-store))]
    (is (empty? (:data/signatures blob)))))


(deftest no-pubkey-blob
  (is (thrown? IllegalStateException
        (-> {:foo 'bar}
            (edn-data/edn-blob
              [(edn-data/typed-map
                 :vault/signature
                 :key (blob/hash (.getBytes "bazbar")))])
            (sig/verify blob-store)))))


(deftest non-pubkey-blob
  (let [non-key (blob/store! blob-store "foobar")]
    (is (thrown? IllegalStateException
          (-> {:foo 'bar}
              (edn-data/edn-blob
                [(edn-data/typed-map
                   :vault/signature
                   :key (:id non-key))])
              (sig/verify blob-store))))))


(deftest signed-blob
  (let [value {:foo "bar", :baz :frobble, :alpha 12345}
        privkeys #(some-> test-keyring
                          (pgp/get-secret-key %)
                          (pgp/unlock-key "test password"))
        blob (-> value
                 (sig/signed-blob blob-store privkeys pubkey-id)
                 (sig/verify blob-store))]
    (is (= :map (:data/type blob)))
    (is (= 2 (count (:data/values blob))))
    (is (= #{pubkey-id} (:data/signatures blob)))))
