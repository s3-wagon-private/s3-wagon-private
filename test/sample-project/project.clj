(defproject s3-wagon-private/test-project "0.0.1-SNAPSHOT"
  :license "no-license"
  :url "https://github.com/s3-wagon-private/s3-wagon-private"
  :description "This is a test repository"
  :plugins [[s3-wagon-private "1.3.1-SNAPSHOT"]]
  :repositories [["testing" {:url "s3p://wagon/snapshots" :no-auth true}]])
