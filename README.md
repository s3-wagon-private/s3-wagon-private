# s3-wagon-private

Deploy and consume artifacts in private S3 repositories. Designed to
be used from [Leiningen](https://github.com/technomancy/leiningen),
but should be usable in other contexts by deploying to repositories at
"s3p://" URLs.

## Usage

### Leiningen 2.x

Add the plugin and repositories listing to `project.clj`:

```clj
:plugins [[s3-wagon-private "1.2.0"]]
```

You can store credentials either in an encrypted file or as
environment variables. For the encrypted file, add this to
`project.clj`:

```clj
:repositories [["private" {:url "s3p://mybucket/releases/" :creds :gpg}]]
```

And in `~/.lein/credentials.clj.gpg`:

```
{"s3p://mybucket/releases" {:username "AKIA2489AE28488"
                            :passphrase "98b0b104ca1211e19a6c"}}
```

The username and passphrase here correspond to the AWS Access Key and Secret
Key, respectively.

The map key here can be either a string for an exact match or a regex
checked against the repository URL if you have the same credentials
for multiple repositories.

To use the environment for credentials, include
`:username :env :passphrase :env` instead of `:creds :gpg` and export
`LEIN_USERNAME` and `LEIN_PASSPHRASE` environment variables.

See `lein help deploying` for details on storing credentials.

If you are running Leiningen in an environment where you don't control
the user such as Heroku or Jenkins, you can include credentials in the
`:repositories` entry. However, you should avoid committing them to
your project, so you should take them from the environment using
`System/getenv`:

```clj
(defproject my-project "1.0.0"
  :plugins [[s3-wagon-private "1.2.0"]]
  :repositories {"releases" {:url "s3p://mybucket/releases/"
                             :username :env/aws_access_key ;; gets environment variable AWS_ACCESS_KEY
                             :passphrase :env/aws_secret_key}}) ;; gets environment variable AWS_SECRET_KEY 
```

### Maven

#### pom.xml

```xml
     <build>
        <extensions>
            <extension>
                <groupId>s3-wagon-private</groupId>
                <artifactId>s3-wagon-private</artifactId>
                <version>1.2.0</version>
            </extension>
        </extensions>
    </build>

    <!-- to publish to a private bucket -->

     <distributionManagement>
                <repository>
                    <id>someId</id>
                    <name>Some Name</name>
                    <url>s3p://some-bucket/release</url>
                </repository>
                <snapshotRepository>
                    <id>someSnapshotId</id>
                    <name>Some Snapshot Name</name>
                    <url>s3p://some-bucket/snapshot</url>
                </snapshotRepository>
     </distributionManagement>

     <!-- to consume artifacts from a private bucket -->

     <repositories>
        <repository>
            <id>someId</id>
            <name>Some Name</name>
            <url>s3p://some-bucket/release</url>
        </repository>
    </repositories>


```

#### settings.xml



```xml


<settings>
    <servers>
        <server>
            <!-- you can actually put the key and secret in here, I like to get them from the env -->
            <id>someId</id>
            <username>${env.AWS_ACCESS_KEY}</username>
            <passphrase>${env.AWS_SECRET_KEY}</passphrase>
        </server>
    </servers>
</settings>

```

### Changes in version 1.2.0

s3-wagon-private is now based on aws-maven 4.8.0-RELEASE, which now uses the
official Amazon S3 client, rather than JetS3t. The list of IAM
permissions required on your S3 bucket have changed, they now include:

 - getBucketLocation
 - listObjects
 - getObject
 - getObjectMetadata
 - putObject (when deploying)
 
## Troubleshooting

If you are seeing errors like: `java.lang.IllegalArgumentException: No matching ctor found for class org.sonatype.aether.repository.Authentication`, and you are using [lein-npm](https://github.com/RyanMcG/lein-npm), try upgrading to version `0.5.1` or later. It contains fixes for [an issue](https://github.com/RyanMcG/lein-npm/pull/13) when using keyword sourced environment variables in your `:repositories`.


## License

Copyright © 2011-2013 Phil Hagelberg, Scott Clasen, Allen Rohner

Based on [aws-maven](http://git.springsource.org/spring-build/aws-maven)
from the Spring project.

Distributed under the Apache Public License version 2.0.
