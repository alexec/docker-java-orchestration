Configuration
===
You have two options for configuration:

* V1 `conf.yml`. One file per app. This is a similar format to `docker.yml`.
* V2 `docker.yml`. A single file that combines all the app's configuration a single file. This is similar to the `docker-compose.yml` format, the keys are the app names, the values are the configuration.

If you specify `conf.yml`, this will override settings in `docker.yml`. You don't need both.

Why used `docker.yml`? We'll try and start the containers in the order you specify (unless we need to start later containers first for dependencies).

The `Dockerfile` are as you'd expect, but **we do property substitution on them**.


V1 `conf.yml` Example
---

```tree
   ├── app1/Dockerfile
   ├── app1/conf.yml
   ├── app2/Dockerfile
   └── app2/conf.yml
```

`conf.yml`:

```yml
# additional data require to create the Docker image
packaging:
  # files to add to the build, usually used with ADD in the Dockerfile
  add:
    - target/example-${project.version}.jar
    # you can also disable filtering
    - path: hello-world.yml
      filter: false
# optional list of port to expose on the host
ports:
  - 8080
  # If you want a different host port used, where the former is the exposed port and the latter the container port.
  - 8001 1802
# containers that this should be linked to, started before this one and stopped afterwards, optional alias after colon
links:
  - mysql:db
healthChecks:
  pings:
    - url: http://localhost:8080/health-check
      timeout: 60000
      pattern: pattern that must be in the body of the return value
# how long in milliseconds to sleep after start-up (default 0)
sleep: 1000
# tag to use for images
tag: alex.e.c/app:${project.artifactId}-${project.version}
# whether or not the app is enabled
enabled: true
# run in privileged mode
privileged: true
# what networkMode to use: bridge, host, none or container (default bridge)
networkMode: bridge
```


V2 `docker.yml` Example
---

```tree
   | docker.yml
   ├── app1/Dockerfile
   └── app2/Dockerfile
```

`docker.yml`:

```yml
app1:
    # app1's conf.yml goes here
    ...
app2:
    # app2's conf.yml goes here
    ...
```