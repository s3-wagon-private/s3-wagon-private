(defproject s3-wagon-private "1.0.1"
  :description "Deploy artifacts to private S3 URLs."
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.springframework.build.aws/org.springframework.build.aws.maven
                  "3.0.0.RELEASE"]
                 [org.apache.maven.wagon/wagon-provider-api "1.0"]]
  :resources-path "res"
  :aot [s3-wagon.private])