(ns metabase.test.data.dynamodb
  (:require [metabase.driver.mongo.util :refer [with-mongo-connection]]
            [metabase.test.data.interface :as i]
            [metabase.util :as u]
            [taoensso.faraday :as far]
            [monger
             [collection :as mc]
             [core :as mg]])
  (:import metabase.driver.mongo.MongoDriver)
  (:import metabase.driver.dynamodb.DynamodbDriver))

(defn- database->connection-details
  ([dbdef]
   {:dbname (i/escaped-name dbdef)
    :host   "localhost"})
  ([_ _ dbdef]
   (database->connection-details dbdef)))

(defn- destroy-db! [dbdef]
  (def client-opts
    {;;; For DDB Local just use some random strings here, otherwise include your
     ;;; production IAM keys:
     :access-key "..."
     :secret-key "..."
     :endpoint (str "http://localhost:8000")}
    )
  (far/delete-table client-opts :categories)
  (far/delete-table client-opts :checkins)
  (far/delete-table client-opts :users)
  (far/delete-table client-opts :venues)
  (with-open [mongo-connection (mg/connect (database->connection-details dbdef))]
    (mg/drop-db mongo-connection (i/escaped-name dbdef))))

(defn- create-db! [{:keys [table-definitions], :as dbdef}]
  (println "WHAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAT")
  (def client-opts
    {;;; For DDB Local just use some random strings here, otherwise include your
     ;;; production IAM keys:
     :access-key "..."
     :secret-key "..."
     :endpoint (str "http://localhost:8000")}
    )
  (println table-definitions)
  (destroy-db! dbdef)
  (with-mongo-connection [mongo-db (database->connection-details dbdef)]
    (println "WHAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAT")
    (doseq [{:keys [field-definitions table-name rows]} table-definitions]
      (far/create-table client-opts table-name
                        [:id :n]  ; Primary key named "id", (:n => number type)
                        {:throughput {:read 1 :write 1} ; Read & write capacity (units/sec)
                         :block? true ; Block thread during table creation
                         })
      (println  (far/list-tables client-opts))
      (let [field-names (for [field-definition field-definitions]
                          (keyword (:field-name field-definition)))]
        ;; Use map-indexed so we can get an ID for each row (index + 1)
        (doseq [[i row] (map-indexed (partial vector) rows)]
          (let [row (for [v row]
                      ;; Conver all the java.sql.Timestamps to java.util.Date, because the Mongo driver insists on being obnoxious and going from
                      ;; using Timestamps in 2.x to Dates in 3.x
                      (if (instance? java.sql.Timestamp v)
                        (java.util.Date. (.getTime ^java.sql.Timestamp v))
                        v))]
            (try
              ;; Insert each row
              (mc/insert mongo-db (name table-name) (assoc (zipmap field-names row)
                                                      :_id (inc i)))
              ;; If row already exists then nothing to do
              (catch com.mongodb.MongoException _))))))))


(u/strict-extend DynamodbDriver
  i/IDriverTestExtensions
  (merge i/IDriverTestExtensionsDefaultsMixin
         {:create-db!                   (u/drop-first-arg create-db!)
          :database->connection-details database->connection-details
          :engine                       (constantly :dynamodb)
          :format-name                  (fn [_ table-or-field-name]
                                          (if (= table-or-field-name "id")
                                            "_id"
                                            table-or-field-name))}))
