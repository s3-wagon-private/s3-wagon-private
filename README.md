# s3-wagon-private

Deploy and consume artifacts in private S3 repositories. Designed to
be used from [Leiningen](https://github.com/technomancy/leiningen),
but should be usable in other contexts by deploying to repositories at
"s3p://" URLs.

> **Note: If this is all too much hassle for you, take a look at [Deps](https://www.deps.co), a simple, private, native Maven repository service that the maintainer also runs.**

## Usage

### Leiningen 2.x

Add the plugin and repositories listing to `project.clj`.

**NB: You need to add these to your `project.clj`, not your personal `~/.lein/profiles.clj`. For more details on why, see Leiningen's doc on [repeatability](https://github.com/technomancy/leiningen/wiki/Repeatability#user-level-repositories)**:

```clj
:plugins [[s3-wagon-private "1.3.5"]]
```

To authenticate to the S3 bucket, you can either use any of the AWS SDK credential providers, store credentials in an encrypted file, or store your credentials in arbitrary environment variables.

#### AWS credential providers

Using one of the AWS SDK [chained provider class][chained-provider-class] credential providers:

Add the following to `project.clj`:

 ```clj
 :repositories [["private" {:url "s3p://mybucket/releases/" :no-auth true}]]
 ```

 An excerpt of the most commonly used credential providers:
 - Environment Variables - `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
 - Java System Properties - `aws.accessKeyId` and `aws.secretKey`
 - Credential profiles file at the default location (~/.aws/credentials) with the [AWS Credentials File Format][credentials-file-format]. To use a particular profile, specify the env var `AWS_PROFILE` or a Java system property of `aws.profile`, otherwise, the fallback will be the default profile name (`"default"`)
 - Instance profile credentials delivered through the Amazon EC2 metadata service


#### Store credentials in an encrypted file

Add the following to `project.clj`:

```clj
:repositories [["private" {:url "s3p://mybucket/releases/" :creds :gpg}]]
```

And in `~/.lein/credentials.clj.gpg`:

```
 {"s3p://mybucket/releases" {:username "AKIA2489AE28488" ;; AWS Access Key
                             :passphrase "98b0b104ca1211e19a6c" ;; AWS Secret Key
                             }}
```

The map key here can be either a string for an exact match or a regex
checked against the repository URL if you have the same credentials
for multiple repositories.

See `lein help deploying` for additional details on storing credentials.

#### Store credentials under arbitrary environment variables

```clj
:repositories {"releases"  {:url "s3p://my-maven/releases/"
                            :username      :env/my_cool_aws_access_key_id
                            :passphrase    :env/my_cool_aws_secret_access_key
                            :sign-releases false}
               "snapshots" {:url           "s3p://my-maven/snapshots/"
                            :username      :env/my_cool_aws_access_key_id
                            :passphrase    :env/my_cool_aws_secret_access_key}}
```

### Maven

#### pom.xml

```xml
     <build>
        <extensions>
            <extension>
                <groupId>s3-wagon-private</groupId>
                <artifactId>s3-wagon-private</artifactId>
                <version>1.3.5</version>
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

     <pluginRepositories>
       <pluginRepository>
         <id>clojars.org</id>
         <name>Clojars Repository</name>
         <url>http://clojars.org/repo</url>
       </pluginRepository>
     </pluginRepositories>

     <repositories>
        <repository>
            <id>someId</id>
            <name>Some Name</name>
            <url>s3p://some-bucket/release</url>
        </repository>
    </repositories>


```

#### settings.xml

This xml is only necessary if not using one of the AWS SDK [chained provider class][chained-provider-class] methods of authentication.

```xml


<settings>
    <servers>
        <server>
            <!-- you can actually put the key and secret in here, I like to get them from the env -->
            <id>someId</id>
            <username>${env.AWS_ACCESS_KEY}</username>
            <privateKey>${user.home}/.ssh/id_rsa</privateKey>
            <passphrase>${env.AWS_SECRET_KEY}</passphrase>
        </server>
    </servers>
</settings>

```

#### AWS Setup

Here's a sample AWS policy that would allow both read and write access to
the bucket `mybucket`:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "S3WagonPrivateAllowConfiguration",
            "Effect": "Allow",
            "Action": [
              "s3:ListBucket",
              "s3:GetBucketLocation"              
            ],
            "Resource": [
              "arn:aws:s3:::mybucket"
            ]
        },
        {
            "Sid": "S3WagonPrivateAllowGetAndPut",
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:GetObjectVersion",
                "s3:PutObject"
            ],
            "Resource": [
                "arn:aws:s3:::mybucket/*"
            ]
        }
    ]
}
```
You may also use the `cloudformation-template.yml` to set your S3 bucket up:

```
export UserArn=$(aws iam get-user --profile $AWS_PROFILE | jq -r '.User.Arn')
aws cloudformation deploy \
  --stack-name s3-maven-repo \
  --template-file cloudformation-template.yml \
  --parameter-overrides BucketName="s3-maven-repo" UserArn="${UserArn}"

```

## Troubleshooting

If you are seeing errors like: `java.lang.IllegalArgumentException: No matching ctor found for class org.sonatype.aether.repository.Authentication`, and you are using [lein-npm](https://github.com/RyanMcG/lein-npm), try upgrading to version `0.5.1` or later. It contains fixes for [an issue](https://github.com/RyanMcG/lein-npm/pull/13) when using keyword sourced environment variables in your `:repositories`.


## Releasing this library

```
# Make sure all of the versions are as you want them
git tag v1.x.y
git push --tags
mvn deploy
# Bump to the next SNAPSHOT version
```

## License

Copyright © 2011-2013 Phil Hagelberg, Scott Clasen, Allen Rohner

Based on [aws-maven](http://git.springsource.org/spring-build/aws-maven)
from the Spring project.

Distributed under the Apache Public License version 2.0.

[chained-provider-class]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html
[credentials-file-format]: http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#aws-credentials-file-format
