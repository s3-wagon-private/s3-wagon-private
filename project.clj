(defproject s3-wagon-private "1.0.0-SNAPSHOT"
  :description "Deploy artifacts to private S3 URLs."
  :dependencies [[org.springframework.build.aws/org.springframework.build.aws.maven
                  "3.0.0.RELEASE"]
                 [org.apache.maven.wagon/wagon-provider-api "1.0"]]
  :resources-path "res"
  :aot [s3-wagon.private]
  ;; plexus will freak out otherwise
  :disable-implicit-clean true)