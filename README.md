# s3-wagon-private

Deploy and consume artifacts in private S3 repositories. Designed to
be used from [Leiningen](https://github.com/technomancy/leiningen),
but should be usable in other contexts by deploying to repositories at
"s3p://" URLs.

## Usage

### Leiningen

Add the plugin and repositories listing to `project.clj`:

```clj
:plugins [[s3-wagon-private "1.1"]]
:repositories {"releases" "s3p://mybucket/releases/"
               "snapshots" "s3p://mybucket/snapshots/"}
```

Versions of Leiningen prior to 1.7.0 don't support `:plugins` in
project.clj; you will need to install by hand:

    $ lein plugin install s3-wagon-private 1.1

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

Copyright © 2011-2012 Phil Hagelberg and Scott Clasen

Based on [aws-maven](http://git.springsource.org/spring-build/aws-maven)
from the Spring project.

Distributed under the Apache Public License version 2.0.
