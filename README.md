Welt Content API Client
=======================

Build
-----

    ./activator clean coverage test
    ./activator coverageReport
    ./activator coverageAggregate

Publish
-------

We are using the [bintray sbt plugin](https://github.com/softprops/bintray-sbt).

You will need a [bintray](https://bintray.com/) account. Log in through sbt:

	./activator bintrayChangeCredentials

Then you can publish new releases with:

	./activator publish

(!) Bump the version, otherwise you will get an error when publishing a version that already exists.
