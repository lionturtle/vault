(ns vault.data
  "Code to handle structured data, usually represented as EDN."
  (:require [clojure.string :as string]
            [clojure.data.codec.base64 :as b64]))


;; TOTAL-ORDERING COMPARATOR

(defn total-order
  "Comparator function that provides a total-ordering of EDN values."
  [a b]
  (compare (pr-str a) (pr-str b))) ; FIXME: dirty hack



;; EDN-TAGGED VALUE PROTOCOL

(defprotocol TaggedValue
  (tag [this] "Return the tag symbol to apply to the value.")
  (value [this] "Return the value to pass to the tag."))



;; SERIALIZATION FUNCTIONS

(defn edn-str
  "Converts the given TaggedValue data to a tagged EDN string."
  ^String
  [v]
  (str \# (tag v) \space (pr-str (value v))))


(defmacro defprint-method
  "Defines a print-method for the given class which writes out the EDN
  serialization from `edn-str`."
  [c]
  `(defmethod print-method ~c
     [v# ^java.io.Writer w#]
       (.write w# (edn-str v#))))



;; BUILT-IN EDN TAGS

; #inst - Date-time instant as an ISO-8601 string.

(defn- format-utc
  "Produces an ISO-8601 formatted date-time string from the given Date."
  [^java.util.Date date]
  (let [date-format (doto (java.text.SimpleDateFormat.
                            "yyyy-MM-dd'T'HH:mm:ss.SSS-00:00")
                       (.setTimeZone (java.util.TimeZone/getTimeZone "GMT")))]
    (.format date-format date)))


(extend-type java.util.Date
  TaggedValue
  (tag [this] 'inst)
  (value [this] (format-utc this)))


; #uuid - Universally-unique identifier string.

(extend-type java.util.UUID
  TaggedValue
  (tag [this] 'uuid)
  (value [this] (str this)))



;; EXPANDED EDN TAG SUPPORT

; #bin - Binary data in the form of byte arrays.

(extend-type (Class/forName "[B")
  TaggedValue
  (tag [this] 'bin)
  (value [this] (->> this b64/encode (map char) (apply str))))


(defprint-method (Class/forName "[B"))


(defn read-bin
  "Reads a base64-encoded string into a byte array."
  ^bytes
  [^String bin]
  (b64/decode (.getBytes bin)))


; #uri - Universal Resource Identifier string.

(extend-type java.net.URI
  TaggedValue
  (tag [this] 'uri)
  (value [this] (str this)))


(defprint-method java.net.URI)


(defn read-uri
  "Constructs a URI from a string value."
  ^java.net.URI
  [^String uri]
  (java.net.URI. uri))
