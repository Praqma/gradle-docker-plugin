package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.TypeCheckingMode
import net.praqma.gradle.docker.jobs.Job
import net.praqma.gradle.docker.tasks.JobBasedTask

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig

@CompileStatic
abstract class DockerObject {

	private DockerHost host
	final DockerObject parent

	@Lazy DockerClient dockerClient = {
		DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
		.withVersion(host.version)
		.withUri(host.uri as String)
		//				.withUsername("dockeruser")
		//				.withPassword("ilovedocker")
		//				.withEmail("dockeruser@github.com")
		// .withServerAddress("https://index.docker.io/v1/")
		.withDockerCertPath(host.certPath.path)
		.build()
		DockerClient client = DockerClientBuilder.getInstance(config).build()
		client
	}()

	DockerObject(DockerObject parent) {
		this.parent = parent
	}

	DockerHost getHost() {
		this.@host ?: parent.host
	}

	void host(Closure closure) {
		if (this.@host == null) this.@host = new DockerHost()
		this.@host.with closure
	}

	Project getProject() {
		parent.project
	}

	DockerPluginExtension getDockerExtension() {
		parent ? parent.dockerExtension : (this as DockerPluginExtension)
	}

	void evalClosureWithDelegate(delegate = null, Closure closure) {
		if (closure) {
			closure.delegate = delegate ?: this
			closure.resolveStrategy = Closure.DELEGATE_FIRST
			closure()
		}
	}

	/** Create a Gradle task, implemeted by executing a Job */
	@CompileStatic(TypeCheckingMode.SKIP)
	Task createJobTask(String name, Class<? extends Job> jobCls, Object ...arguments) {
		Closure configBlock = null
		// TODO look into: It seems project.tasks.create(name, JobBasedTask) return null?!?
		Task task = project.tasks.create(name: name, type: JobBasedTask) {
			if (arguments.size() > 0 && arguments[-1] instanceof Closure) {
				configBlock = arguments[-1]
				arguments = arguments[0..-2]
			}
			group 'Docker'
			jobClass jobCls
			args arguments
		}
		assert task != null
		if (configBlock) task.configure configBlock
		task
	}

	Map<String, String> dockerVersion() {
		dockerClient.versionCmd().exec().properties
	}

	Logger getLogger() {
		project.logger
	}

	protected void postProcess() {
	}

	ContainerInfo findContainerByName(String fullName) {
		Collection<Container> containerList = dockerClient.listContainersCmd().withShowAll(true).exec() as Collection<Container>
		Container c = containerList.find { Container c ->
			c.names.contains("/${fullName}")
		}
		if (c) {
			new ContainerInfo(c.id, c.image)
		} else {
			null
		}
	}
}

@Immutable
@CompileStatic
class ContainerInfo {
	String id
	String imageId
}
