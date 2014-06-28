(ns vault.index.catalog
  (:require
    [clj-time.core :as time]
    [vault.blob.store :as store]
    [vault.index.engine :as engine]))


;;;;; HELPER FUNCTIONS ;;;;;

(defn- get-blob-index
  [catalog]
  (get (:indexes catalog) (:blob-key catalog)))



;;;;; INDEX CATALOG ;;;;;

(defrecord IndexCatalog
  [indexes blob-key])

(extend-type IndexCatalog
  store/BlobStore

  (enumerate
    [this opts]
    (store/select-ids opts
      ; TODO: this is where being able to do real queries would help;
      ; specifically, for :after and :prefix.
      (engine/search (get-blob-index this) {})))


  (stat
    [this id]
    (when id
      (-> (get-blob-index this)
          (engine/search {:blob id})
          first)))


  (put!
    [this blob]
    (when-not (store/stat this (:id blob))
      ; TODO: ensure blob has been parsed?
      (doseq [index (vals (:indexes this))]
        (when-let [projection (:projection index)]
          (doseq [record (projection blob)]
            (engine/update! index record)))))
    blob))


(defn catalog
  "Creates a new catalog out of the given indexes. The keyword given names
  the index to look up blob stats from."
  [indexes blob-key]
  {:pre [(contains? indexes blob-key)]}
  (IndexCatalog. indexes blob-key))
