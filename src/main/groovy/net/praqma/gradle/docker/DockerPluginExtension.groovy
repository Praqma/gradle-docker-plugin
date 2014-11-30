package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic

import org.gradle.api.Project

import com.github.dockerjava.api.model.Container

@CompileStatic
class DockerPluginExtension extends DockerDslObject implements CompositeCompute {

	final Project project
	private final NamedObjects<LocalDockerImage> images

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
	}

	LocalDockerImage image(String name, Closure configBlock = null) {
		images.getObject(name, LocalDockerImage, configBlock)
	}

	void postProcess() {
		//images.each { LocalDockerImage img -> img.postProcess() }
		traverse(DockerCompute) { DockerCompute c -> c.postProcess() }
	}

}
