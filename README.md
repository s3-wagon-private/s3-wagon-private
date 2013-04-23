# s3-wagon-private

Deploy and consume artifacts in private S3 repositories. Designed to
be used from [Leiningen](https://github.com/technomancy/leiningen),
but should be usable in other contexts by deploying to repositories at
"s3p://" URLs.

## Usage

### Leiningen 2.x

Add the plugin and repositories listing to `project.clj`:

```clj
:plugins [[s3-wagon-private "1.1.2"]]
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

The map key here can be either a string for an exact match or a regex
checked against the repository URL if you have the same credentials
for multiple repositories.

To use the environment for credentials, include
`:username :env :passphrase :env` instead of `:creds :gpg` and export
`LEIN_USERNAME` and `LEIN_PASSPHRASE` environment variables.

See `lein help deploying` for details on storing credentials.

Currently in Leiningen 2 you have to manually activate the plugin with
the following form at the bottom of project.clj:

```clj
(cemerick.pomegranate.aether/register-wagon-factory!
 "s3p" #(eval '(org.springframework.aws.maven.PrivateS3Wagon.)))
 ```

Future versions should remove the need for this declaration.

### Leiningen 1.x

As above, add the plugin and repositories listing to `project.clj`:

```clj
:plugins [[s3-wagon-private "1.1.2"]]
:repositories {"private" {:url "s3p://mybucket/releases/"}}
```

On 1.x you keep S3 credentials in `~/.lein/init.clj`:

```clj
(def leiningen-auth {"s3p://mybucket/releases/"
                     {:username "ACCESS_KEY"
                      :passphrase "SECRET_KEY"}
                     "s3p://mybucket/snapshots/"
                     {:username "ACCESS_KEY"
                      :passphrase "SECRET_KEY"}})
```

Note that deploying an artifact that doesn't already exist will cause
an `org.jets3t.service.S3ServiceException` stack trace to be emitted;
this is a bug in one of the underlying libraries but is harmless.

If you are running Leiningen in an environment where you don't control
the user such as Heroku or Jenkins, you can include credentials in the
`:repositories` entry. However, you should avoid committing them to
your project, so you should take them from the environment using
`System/getenv`:

```clj
(defproject my-project "1.0.0"
  :plugins [[s3-wagon-private "1.1.2"]]
  :repositories {"releases" {:url "s3p://mybucket/releases/"
                             :username (System/getenv "AWS_ACCESS_KEY")
                             :passphrase (System/getenv "AWS_SECRET_KEY")}})
```

### Maven

#### pom.xml

```xml
     <build>
        <extensions>
            <extension>
                <groupId>s3-wagon-private</groupId>
                <artifactId>s3-wagon-private</artifactId>
                <version>1.1.2</version>
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


## License

Copyright © 2011-2012 Phil Hagelberg and Scott Clasen

Based on [aws-maven](http://git.springsource.org/spring-build/aws-maven)
from the Spring project.

Distributed under the Apache Public License version 2.0.
