package net.praqma.docker.connection

import static net.praqma.docker.connection.HostSpec.DOCKER_HOST_ENVVAR_NAME
import static net.praqma.docker.connection.HostSpec.DOCKER_HOST_PROPERTY_NAME

interface DockerHostDiscoverer {

    Map discover()

    static DockerHostDiscoverer systemProperties = {
        Utils.fromMap(System.properties)
    }

    static DockerHostDiscoverer environmentVariables = {
        Utils.fromMap(System.getenv())
    }

    static class Utils {
        static Map<String, String> fromMap(Map<String, String> m) {
            (m[DOCKER_HOST_PROPERTY_NAME] || m[DOCKER_HOST_ENVVAR_NAME]) ? m : null
        }
    }
}