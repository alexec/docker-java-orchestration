Change Log
===
2.4.0

* Added the ability to verify the body of a ping health-check.

2.3.1

* Fix splitting of tag name when tag contains port declaration for non-default registry.

2.3.0

* [DJO Pull 15](https://github.com/alexec/docker-java-orchestration/pull/15) Add method to get ID addresses.
* Changed to Apache 2.0 licence.
* [DJO Issue 32](https://github.com/alexec/docker-maven-plugin/issues/32) Added support for private repositories. 

2.2.0

* Provide a plugin system, and a boot2docker plugin that sets up port fowarding automatically.
* [DJO Issue 30](https://github.com/alexec/docker-maven-plugin/issues/30) Use version of docker-java that supports SSL.
* Improved error reporting on push.

2.1.0

* Fixed bug preventing correct linking of containers.
* [DMP Issue 26](https://github.com/alexec/docker-maven-plugin/issues/26) Support for link alias.
* Forcibly delete images.

2.0.2

* [DMP Pull 27](https://github.com/alexec/docker-maven-plugin/pull/27) Added environment and system properties.

2.0.1

* [DJO Pull 10](https://github.com/alexec/docker-java-orchestration/pull/10) Correctly preserve line endings on Windows. 
* [DJO Pull 11](https://github.com/alexec/docker-java-orchestration/pull/11) Use docker-java 0.10.2.
* [DJO Pull 12](https://github.com/alexec/docker-java-orchestration/pull/12) Correctly stop containers.


2.0.0

* Updated to use port 2375 by default.
* [DJO Issue 2](https://github.com/alexec/docker-java-orchestration/issues/2) Support for volumes.
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

