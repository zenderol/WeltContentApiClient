![Logo](static/img/wcac_logo.png)

Welt Content API Client
=======================

Build
-----

    ./activator clean coverage test
    ./activator coverageReport
    ./activator coverageAggregate

Local Development (with an ApiClient-Project)
---------------------------------------------

There is no need to do a full dev cycle (publish to bintray and bump the version in the other project)
each time something changes. For this use case you can benefit from _SNAPSHOT_ releases. Set the version to
`version := "0.2.1-SNAPSHOT",` and then do a

    ./activator publishLocal

in the other project, enable resolving vom local caches by adding a resolver `Resolver.mavenLocal` and also
switch to the same _SNAPSHOT_ version.

Publish
-------
Publishing Versions is done by commiting to master. CI [Jenkins](https://jenkins.up.welt.de/job/funkotron/job/WeltContentApiClient/job/WeltContentApiClientPipeline/) will take care of the rest for you.

(!) Bump the version in file `MyBuild.scala`, otherwise you will get an error when publishing a version that already exists.

We are using the [bintray sbt plugin](https://github.com/softprops/bintray-sbt).

Deploying from local
--------------------
You will need a [bintray](https://bintray.com/) account. Log in through sbt:

	./activator bintrayChangeCredentials

Then you can publish new releases with:

	./activator publish