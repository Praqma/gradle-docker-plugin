package net.praqma.gradle.docker

import org.gradle.api.Project

import com.github.dockerjava.api.model.Container

//@CompileStatic
class DockerPluginExtension extends DockerDslObject {

	final Project project
	private final NamedObjects<DockerAppliance, DockerPluginExtension> appliances
	private final NamedObjects<LocalDockerImage, DockerPluginExtension> images

	DockerPluginExtension(Project project) {
		super("___${getClass().name}___", null)
		this.project = project
		this.appliances = new NamedObjects<>(this, DockerAppliance)
		this.images = new NamedObjects<>(this, LocalDockerImage)
		host { } // host.uri is lazy initialized to a meaningful value
		getHost()
		project.afterEvaluate { this.postProcess() }
		project.tasks.create(name: 'dockerVersion', group: 'Docker') {
			description 'Display version information about the Docker host'
			doLast {
				def m = dockerVersion()
				['apiVersion', 'version', 'kernelVersion', 'goVersion', 'gitCommit'].each { String key ->
					println "${key} = ${m[key]}"
				}
			}
		}
	}

	DockerAppliance appliance(String name, Closure configBlock = null) {
		appliances.get(name, configBlock)
	}

	LocalDockerImage image(String name, Closure configBlock = null) {
		images.get(name, configBlock)
	}

	void assignAllContanerIds() {
		Map<String,String> name2id = [:]
		Collection<Container> containerList = dockerClient.listContainersCmd().withShowAll(true).exec() as Collection<Container>
		containerList.each { Container c ->
			c.names.each { String n ->
				assert n[0] == '/'
				name2id[n.substring(1)] = c.id
			}
		}
		this.appliances.each { DockerAppliance a ->
			a.containers.each { DockerContainer c ->
				String id = name2id[c.fullName]
				if (id != null) {
					c.containerId = id
				}
			}
		}

	}

	@Override
	void postProcess() {
		super.postProcess()
		images.postProcess()
		appliances.postProcess()
	}

}
