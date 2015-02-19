package net.praqma.docker.connection

import groovy.transform.CompileStatic
import net.praqma.docker.connection.DockerHostDiscoverer
import org.gradle.api.Project
import org.slf4j.Logger

@CompileStatic
class Boot2DockerDiscoverer implements DockerHostDiscoverer {

    private Logger logger
    private String boot2dockerBin

    Boot2DockerDiscoverer(Logger logger, boot2dockerBin = null) {
        this.logger = logger
        this.boot2dockerBin = boot2dockerBin ?: System.properties['net.praqma.boot2dockerBin'] ?: 'boot2docker'
    }

    @Override
    Map<String, String> discover() {
        ProcessBuilder pb = new ProcessBuilder(boot2dockerBin, 'shellinit')
        Process p
        try {
            p = pb.start()
        } catch (IOException e) {
            logger.warn "boot2docker executable not found: ${boot2dockerBin}"
            return null
        }
        p.waitFor()
        if (p.exitValue() == 0) {
            logger.info "Setting HostSpec from boot2docker"
            def a = p.inputStream.readLines().collect { it.trim().replaceFirst(/^export /, '').split('=') }
            a.collectEntries { String[] pair -> [pair[0], pair[1]] }
        } else {
            logger.warn "boot2docker error: ${p.errorStream.text}"
            null
        }
    }
}
