#!/bin/bash -ex

case "$1" in
    pre_machine)

        mkdir .docker
        cp $CIRCLE_PROJECT_REPONAME/etc/certs/* .docker

        docker_opts='DOCKER_OPTS="$DOCKER_OPTS -H tcp://127.0.0.1:2376 --tlsverify --tlscacert='$HOME'/.docker/ca.pem --tlscert='$HOME'/.docker/server-cert.pem --tlskey='$HOME'/.docker/server-key.pem"'
        sudo sh -c "echo '$docker_opts' >> /etc/default/docker"
        ;;
    post_machine)
        docker version
        ;;
esac
