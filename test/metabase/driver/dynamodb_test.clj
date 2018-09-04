(ns metabase.driver.dynamodb-test
  "Tests for DynamoDB driver."
  (:require [expectations :refer :all]
            [medley.core :as m]
            [metabase.automagic-dashboards.core :as magic]
            [metabase
             [driver :as driver]
             [query-processor :as qp]
             [query-processor-test :refer [rows]]]
            [metabase.driver.mongo :as mongo]
            [metabase.driver.mongo.query-processor :as mongo-qp]
            [metabase.models
             [field :refer [Field]]
             [table :as table :refer [Table]]]
            [metabase.query-processor.middleware.expand :as ql]
            [metabase.test.data :as data]
            [metabase.test.data
             [datasets :as datasets]
             [interface :as i]]
            [toucan.db :as db])
  (:import metabase.driver.dynamodb.DynamodbDriver
           org.bson.types.ObjectId
           org.joda.time.DateTime))

;; ## Constants + Helper Fns/Macros
;; TODO - move these to metabase.test-data ?
(def ^:private ^:const table-names
  "The names of the various test data `Tables`."
  [:categories
   :checkins
   :users
   :venues])

;; ## Tests for connection functions

;(datasets/expect-with-engine :dynamodb
;  false
;  (driver/can-connect-with-details? :dynamodb {:host   "localhost"
;                                            :port   3000
;                                            :dbname "bad-db-name"}))
;
;(datasets/expect-with-engine :dynamodb
;  false
;  (driver/can-connect-with-details? :dynamodb {}))
;
;(datasets/expect-with-engine :dynamodb
;  true
;  (driver/can-connect-with-details? :dynamodb {:host "localhost"
;                                                :port 8000
;                                                :dbname "metabase-test"}))
;
;;; should use default port 27017 if not specified
;(datasets/expect-with-engine :dynamodb
;  true
;  (driver/can-connect-with-details? :dynamodb {:host "localhost"
;                                               :port 8000
;                                               :other 1
;                                            :dbname "metabase-test"}))
;
;(datasets/expect-with-engine :dynamodb
;  false
;  (driver/can-connect-with-details? :dynamodb {:host "123.4.5.6"
;                                            :dbname "bad-db-name?connectTimeoutMS=50"}))
;
;(datasets/expect-with-engine :dynamodb
;  false
;  (driver/can-connect-with-details? :dynamodb {:host "localhost"
;                                            :port 3000
;                                            :dbname "bad-db-name?connectTimeoutMS=50"}))

;; DESCRIBE-DATABASE
(datasets/expect-with-engine :dynamodb
                             {:tables #{{:schema nil, :name "checkins"}
                                        {:schema nil, :name "categories"}
                                        {:schema nil, :name "users"}
                                        {:schema nil, :name "venues"}}}
                             (driver/describe-database (DynamodbDriver.) (data/db)))
