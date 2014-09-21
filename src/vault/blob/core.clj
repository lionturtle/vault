(ns vault.blob.core
  (:refer-clojure :exclude [get hash list read])
  (:require
    [potemkin :refer [import-vars]]
    (vault.blob
      digest
      store)))


(import-vars
  (vault.blob.digest
    hash
    hash-id
    parse-id
    path-str)
  (vault.blob.store
    record
    read
    write
    list
    stat
    get
    get'
    put!
    store!
    delete!))
