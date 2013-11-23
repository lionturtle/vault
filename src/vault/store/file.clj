(ns vault.store.file
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [vault.blob :as blob :refer [BlobStore]]))


;; HELPER FUNCTIONS

(defn- blobref->file
  ^java.io.File
  [root blobref]
  (let [blobref (blob/ref blobref)
        {:keys [algorithm digest]} blobref]
    (io/file root
             (name algorithm)
             (subs digest 0 3)
             (subs digest 3 6)
             (subs digest 6))))


(defn- file->blobref
  [root file]
  (let [root (str root)
        file (str file)]
    (when-not (.startsWith file root)
      (throw (IllegalArgumentException.
               (str "File " file " is not a child of root directory " root))))
    (let [[algorithm & digest] (-> file
                                   (subs (inc (count root)))
                                   (string/split #"/"))]
      (blob/ref algorithm (string/join digest)))))


; As a long-term idea, this could try to buffer in memory up to a certain
; threshold before spooling to disk.
(defn- spool-tmp-file!
  "Spool input stream to a temporary landing file."
  ^java.io.File
  [root content]
  (let [tmp-file (io/file root "tmp" (str "landing-" (System/currentTimeMillis)))]
    (io/make-parents tmp-file)
    (io/copy content tmp-file)
    tmp-file))


(defn- store-blob-file!
  "Integrates a landing file into the blob store."
  ^java.io.File
  [^java.io.File tmp
   ^java.io.File file]
  (io/make-parents file)
  (when-not (.renameTo tmp file)
    (throw (RuntimeException.
             (str "Failed to rename landing file " tmp
                  " to stored blob " file))))
  (.setWritable file false false))


(defn- probe-content-type
  "Attempts to provide content-type information for a stored blob. Returns a
  MIME string on success."
  [^java.io.File file]
  (java.nio.file.Files/probeContentType (.toPath file)))


(defmacro ^:private for-files
  [[sym dir] expr]
  `(let [files# (->> ~dir .listFiles sort)
         f# (fn [~(vary-meta sym assoc :tag 'java.io.File)] ~expr)]
     (map f# files#)))


(defn- enumerate-files
  "Generates a lazy sequence of file blobs contained in a root directory."
  [^java.io.File root]
  ; TODO: intelligently skip entries based on 'start'
  (flatten
    (for-files [algorithm-dir root]
      (for-files [prefix-dir algorithm-dir]
        (for-files [midfix-dir prefix-dir]
          (for-files [blob midfix-dir]
            blob))))))



;; FILE STORE

(defrecord FileBlobStore
  [algorithm
   ^java.io.File root]

  BlobStore

  (-algorithm [this]
    algorithm)


  (-list [this opts]
    (->> (enumerate-files root)
         (map (partial file->blobref root))
         (blob/select-refs opts)))


  (-stat [this blobref]
    (let [file (blobref->file root blobref)]
      (when (.exists file)
        {:size (.length file)
         :since (java.util.Date. (.lastModified file))
         :content-type (probe-content-type file)
         :location (.toURI file)})))


  (-open [this blobref]
    (let [file (blobref->file root blobref)]
      (when (.exists file)
        (io/input-stream file))))


  (-store! [this content]
    (let [tmp (spool-tmp-file! root content)
          blobref (blob/digest algorithm tmp)
          file (blobref->file root blobref)]
      (if (.exists file)
        (.delete tmp)
        (store-blob-file! tmp file))
      blobref))


  (-remove! [this blobref]
    (let [file (blobref->file root blobref)]
      (when (.exists file)
        (.delete file)))))


(defn file-store
  "Creates a new local file-based blob store."
  [algorithm root]
  (FileBlobStore. algorithm
                  (io/file root)))
