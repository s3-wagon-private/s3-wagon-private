# s3-wagon-private

Deploy artifacts to private S3 repositories. Designed to be used from
[Leiningen](https://github.com/technomancy/leiningen), but should be
usable in other contexts by deploying to repositories at "s3p://" URLs.

## Usage

If you're using this from Leiningen 1.6.2 or older, there are some
bootstrapping issues that prevent this from being usable in
`:dev-dependencies`. For the time being it must be installed as a
user-level plugin:

    $ lein plugin install s3-wagon-private 1.0.0

Add the repositories listing to `project.clj`:

```clj
:repositories {"releases" "s3p://mybucket/releases/"
               "snapshots" "s3p://mybucket/snapshots/"}
```

You should keep your S3 credentials in `~/.lein/init.clj`:

```clj
(def leiningen-auth {"s3p://mybucket/releases/"
                     {:username "ACCESS_KEY"
                      :passphrase "SECRET_KEY"}
                     "s3p://mybucket/snapshots/"
                     {:username "ACCESS_KEY"
                      :passphrase "SECRET_KEY"}})
```

This will allow you to both read and write to/from S3 buckets as Maven
repositories. Note that deploying an artifact that doesn't already
exist will cause an `org.jets3t.service.S3ServiceException` stack
trace to be emitted; this is a bug in one of the underlying libraries
but is harmless.

## License

Copyright (C) 2011 Phil Hagelberg

Based on [aws-maven](http://git.springsource.org/spring-build/aws-maven)
from the Spring project.

Distributed under the Eclipse Public License, the same as Clojure.
