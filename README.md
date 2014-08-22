[![Build Status](https://travis-ci.org/alexec/docker-java-orchestration.svg?branch=master)](https://travis-ci.org/alexec/docker-java-orchestration)

A small library to orchestrate groups of Docker containers using YAML configuration.

Change Log
---
2.0.0

* Updated to use port 2375 by default.

1.4.0

* Support disabling of image caching.

1.3.1

* Fix issue with logging binary to console.

1.3.0

* Tag images.

1.2.0

* Remove intermediate containers.
* Support Windows.
 
1.1.0

* Pings containers to see if they are up before returning.
* Added a method the see if the container group is running.
* Will not attempt to start a running container (more idempotent).
* Fixed bug whereby the wrong container may be started.

1.0.2

* First release.

Contributors
---
* Alex Collins
* Dan Jasek
* Jacob Bay Hansen
* Lachlan Coote
* Laurie Hallowes
* Panu Wetterstrand

