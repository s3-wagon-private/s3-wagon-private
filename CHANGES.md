# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [1.3.3]

### Changed

* Add aws-java-sdk-sts to support AWS STS AssumeRole
* Updated AWS SDK to 1.11.579 [#73](https://github.com/s3-wagon-private/s3-wagon-private/pull/73)
* Updated Jackson dependencies to 2.9.9 [#65](https://github.com/s3-wagon-private/s3-wagon-private/pull/65)


## [1.3.2]

* Bump Jackson dependencies to match AWS versions.
* Bump AWS dependency to avoid `java.lang.ClassNotFoundException: javax.xml.bind.JAXBException` issues with Java 10.

## [1.3.1]

* Removes unnecessary creation of bucket prefixes in S3. This gives users the ability to narrow the scope AWS Permissions for artifact uploads.
* Adds support for Leiningen 2.8.0 and up 

## [1.3.0] - 2017-01-31

This is a major upgrade that adds support for the AWS SDK provider chain. There is the possibility that this new version won't be compatible with your tooling, due to classpath issues with Jackson. If this is the case, please open an issue and we will try to fix it.

### Changed

- Use the AWS SDK provider chain which adds support for IAM rules, STS tokens, and all of the other creamy authentication goodness that AWS can cook up.
- Add explicit dependency on AWS Java SDK 1.11.28
- Add docs on how to use the new provider chain

## [1.2.0]

s3-wagon-private is now based on aws-maven 4.8.0-RELEASE, which now uses the
official Amazon S3 client, rather than JetS3t. The list of IAM
permissions required on your S3 bucket have changed, they now include:

 - getBucketLocation
 - listObjects
 - getObject
 - getObjectMetadata
 - putObject (when deploying)

Here's a sample AWS policy that would allow both read and write access to
the bucket `mybucket`:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "StmtXXXXX",
            "Effect": "Allow",
            "Action": [
                "s3:GetBucketLocation",
                "s3:GetObject",
                "s3:GetObjectVersion",
                "s3:ListBucket",
                "s3:ListObjects",
                "s3:PutObject"
            ],
            "Resource": [
                "arn:aws:s3:::mybucket",
                "arn:aws:s3:::mybucket/*"
            ]
        }
    ]
}
```
