package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic
import net.praqma.docker.connection.Boot2DockerDiscoverer
import net.praqma.docker.connection.DockerHostDiscoverer
import net.praqma.docker.connection.HostConnection
import net.praqma.docker.connection.HostSpec

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

@CompileStatic
class DockerPluginExtension extends DockerDslObject implements CompositeCompute {

    private final NamedObjects<LocalDockerImage> images

    final HostSpec host = new HostSpec()

    final HostConnection connection = new HostConnection()

    final Project project

    void host(Closure closure) {
        host.with closure
    }

    @CompileDynamic
    class BuildListener extends BuildAdapter {
        void buildFinished(BuildResult result) {
            connection.shutdown()
        }

        void projectsEvaluated(Gradle gradle) {
            startDockerConnection()
        }
    }

    @CompileDynamic
    DockerPluginExtension(Project project) {
        super("DockerPluginExtension", null)
        this.project = project
        initCompositeCompute(this)
        this.images = new NamedObjects<>(this)
        project.afterEvaluate { postProcess() }
        project.tasks.create(name: 'dockerVersion', group: 'Docker') {
            description 'Display version information about the Docker host'
            doLast {
                def m = dockerVersion()
                [
                        'apiVersion',
                        'version',
                        'kernelVersion',
                        'goVersion',
                        'gitCommit'
                ].each { String key -> println "${key} = ${m[key]}" }
            }
        }
        project.gradle.addBuildListener(new BuildListener())

        host.discoveres.addAll(DockerHostDiscoverer.systemProperties,
                DockerHostDiscoverer.environmentVariables,
                new Boot2DockerDiscoverer(project.logger))
    }

    void startDockerConnection() {
        if (host.autoDiscover && host.uri == null) {
            host.discover()
        }
        connection.start(host, project.logger)
    }

    LocalDockerImage image(String name, Closure configBlock = null) {
        images.getObject(name, LocalDockerImage, configBlock)
    }

    LocalDockerImage getImage(String name) {
        images.get(name)
    }

    void postProcess() {
        images.each { LocalDockerImage img -> img.postProcess() }
        //traverse(DockerCompute) { DockerCompute c -> c.postProcess() }
    }

}
