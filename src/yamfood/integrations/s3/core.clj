(ns yamfood.integrations.s3.core
  (:require
    [environ.core :refer [env]])
  (:use
    [amazonica.core]
    [amazonica.aws.s3])
  (:import
    (org.joda.time DateTime)))


(def bucket (env :bucketeer-bucket-name))


(def cred
  {:access-key (env :bucketeer-aws-access-key-id)
   :secret-key (env :bucketeer-aws-secret-access-key)
   :endpoint   (env :bucketeer-aws-region)})


(def date (.plusDays (DateTime.) 1))


(defcredential
  (:access-key cred)
  (:secret-key cred)
  (:endpoint cred))


(defn generate-presigned-url!
  [key]
  (generate-presigned-url {:bucket-name        bucket
                           :key                key
                           :date               date
                           :method             "PUT"
                           :request-parameters {:x-amz-acl "public-read"}}))
