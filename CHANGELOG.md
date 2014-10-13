Change Log
===
2.0.1

* [DJO Pull 10](https://github.com/alexec/docker-java-orchestration/pull/10) Correctly preserve line endings on Windows. 
* [DJO Pull 11](https://github.com/alexec/docker-java-orchestration/pull/11) Use docker-java 0.10.2.
* [DJO Pull 12](https://github.com/alexec/docker-java-orchestration/pull/12) Correctly stop containers.


2.0.0

* Updated to use port 2375 by default.
* [DJO Issue 2][https://github.com/alexec/docker-java-orchestration/issues/2] Support for volumes.
* [DMP Issue 8](https://github.com/alexec/docker-maven-plugin/issues/8) Correct incorrect tag names.
* [DMP Issue 22](https://github.com/alexec/docker-maven-plugin/issues/22) Use docker-java.

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

