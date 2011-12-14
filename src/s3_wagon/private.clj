(ns s3-wagon.private
  (:import (org.jets3t.service.model S3Object)
           (org.jets3t.service.acl AccessControlList)))

(gen-class :name s3.wagon.private.PrivateWagon
           :extends org.springframework.aws.maven.SimpleStorageServiceWagon)

(defn get-field [this field]
  (-> org.springframework.aws.maven.SimpleStorageServiceWagon
      (.getDeclaredField field)
      (doto (.setAccessible true))
      (.get this)))

(defn -putResource [this source destination progress]
  (let [object (doto (S3Object. (str (get-field this "basedir") destination))
                 (.setAcl AccessControlList/REST_CANNED_PRIVATE)
                 (.setDataInputFile source)
                 (.setContentLength (.length source)))]
    (-> (get-field this "service")
        (.putObject (get-field this "bucket") object))))
