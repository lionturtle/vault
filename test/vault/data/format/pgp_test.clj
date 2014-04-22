(ns vault.data.format.pgp-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [mvxcvi.crypto.pgp :as pgp]
    [mvxcvi.crypto.pgp.keyring :as keyring]
    [vault.blob.core :as blob]
    [vault.data.format.pgp :as pgp-data]))


(def test-keyring
  (keyring/pgp-keyring
    (io/resource "test-resources/pgp/pubring.gpg")
    (io/resource "test-resources/pgp/secring.gpg")))

(def pubkey
  (pgp/get-public-key test-keyring "923b1c1c4392318a"))



(deftest pgp-typing
  (is (= nil (pgp-data/pgp-type nil)))
  (is (= :pgp/public-key (pgp-data/pgp-type pubkey))))


(deftest pgp-blobs
  (let [blob (pgp-data/pgp-blob pubkey)]
    (is (= :pgp/public-key (:data/type blob)))
    (is (= [pubkey] (:data/values blob)))
    (let [raw-blob (select-keys blob [:id :content])
          parsed-blob (pgp-data/read-blob raw-blob)]
      (is (= :pgp/public-key (:data/type blob)))
      (is (= [pubkey] (:data/values blob))))))