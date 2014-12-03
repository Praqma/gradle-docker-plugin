package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic
import net.praqma.docker.connection.HostConnection
import net.praqma.docker.connection.HostSpec

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Project

@CompileStatic
class DockerPluginExtension extends DockerDslObject implements CompositeCompute {

	private final NamedObjects<LocalDockerImage> images

	final HostSpec host = new HostSpec()

	final HostConnection connection = new HostConnection()
	
	final Project project

	void host(Closure closure) {
		host.with closure
	}

	@CompileStatic
	class BuildListener extends BuildAdapter {
		void buildFinished(BuildResult result) {
			connection.shutdown()
		}
	}
	
	@CompileDynamic
	DockerPluginExtension(Project project) {
		super("DockerPluginExtension", null)
		this.project = project
		initCompositeCompute(this)
		this.images = new NamedObjects<>(this)
		host { } // host.uri is lazy initialized to a meaningful value
		getHost()
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
		connection.start(host, project.logger)
	}

	LocalDockerImage image(String name, Closure configBlock = null) {
		images.getObject(name, LocalDockerImage, configBlock)
	}

	void postProcess() {
		//images.each { LocalDockerImage img -> img.postProcess() }
		traverse(DockerCompute) { DockerCompute c -> c.postProcess() }
	}

}
