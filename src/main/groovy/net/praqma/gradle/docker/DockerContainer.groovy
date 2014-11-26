package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic
import groovy.transform.Immutable
import groovy.transform.TypeCheckingMode
import net.praqma.gradle.docker.jobs.ContainerJob

import org.gradle.api.GradleException
import org.gradle.api.Project

import com.github.dockerjava.api.NotModifiedException
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.StartContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Link
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.Volume


@CompileStatic
class DockerContainer extends DockerDslObject implements DockerComputeTrait {

	private Collection<DockerPortBinding> portBindings = [] as Set
	private Collection<String> volumes = [] as Set
	private Collection<String> volumesFrom = [] as Set
	private Collection<LinkInfo> links = [] as Set
	private Map<String, String> env = [:]

	final private RemoteDockerImage image
	String localImage

	private String containerId
	@CompileDynamic
	DockerContainer(String name, DockerAppliance appliance) {
		super(name, appliance)
		this.image = new RemoteDockerImage(this)
		prepareTask = project.tasks.create(name: taskName('Prepare'))
		appliance.prepareTask.dependsOn prepareTask
	}

	String taskName(String task) {
		"container${fullName}${task.capitalize()}"
	}

	void setLocalImage(String liName) {
		this.@localImage = liName
		prepareTask.dependsOn LocalDockerImage.copyTaskName(liName)
	}

	DockerAppliance getAppliance() {
		parent as DockerAppliance
	}

	void image(String image) {
		String[] parts = image.split(':')
		switch (parts.length) {
			case 1:
				this.image.repository = parts[0]
				break
			case 2:
				this.image.repository = parts[0]
				this.image.tag = parts[1]
				break
			default:
				throw new GradleException()
		}
	}

	void image(Closure closure) {
		this.image.with closure
	}

	String getFullName() {
		calculateFullName(name)
	}

	void env(Map<String,String> m) {
		this.env.putAll(m)
	}

	void volume(String volume) {
		this.volumes << volume
	}

	void volumesFrom(DockerContainer container) {
		volumesFrom(container.name)
	}

	void volumesFrom(String name) {
		volumesFrom << name
	}

	void link(DockerContainer container, String alias = null) {
		link(container.name, alias)
	}

	void link(String containerName, String alias = containerName) {
		links << new LinkInfo(containerName, alias)
	}

	Project getProject() {
		appliance.project
	}

	String create(String imageId) {
		if (containerId == null) {
			logger.info "Creating Docker container from ${imageId}"
			CreateContainerCmd cmd = dockerClient.createContainerCmd(imageId)
					.withName(fullName)
					.withVolumes(volumes.collect { new Volume(it as String) } as Volume[])
					.withEnv(map2StringArray(env))
			CreateContainerResponse resp = cmd.exec()
			assert resp.id != null
			if (resp.warnings) {
				resp.warnings.each { println it }
			}
			containerId = resp.id
		}
		return containerId
	}

	def start() {
		assert this.containerId != null
		logger.warn "Starting Docker container from image ${image}"
		Ports ports = new Ports()
		portBindings.each { DockerPortBinding binding ->
			ports.bind(ExposedPort.tcp(binding.containerPort), new Ports.Binding(binding.hostPort))
		}

		StartContainerCmd cmd = dockerClient.startContainerCmd(containerId)
				.withLinks(links.collect { LinkInfo li -> new Link(calculateFullName(li.name), li.alias) } as Link[])
				.withPortBindings(ports)
		// TODO set more volumes
		if (this.volumesFrom.size() > 0) {
			cmd.withVolumesFrom(calculateFullName(volumesFrom.first()))
		}
		try {
			cmd.exec()
		} catch (NotModifiedException e) {
			// container already started
			// TODO make sure it is configured as desired
		}
	}

	def kill() {
		if (containerId != null) {
			dockerClient.killContainerCmd(containerId).exec()
		}
	}

	def remove() {
		if (containerId != null) {
			String id = containerId
			this.containerId = null
			dockerClient.removeContainerCmd(id).exec()
		}
	}

	def stop() {
		if (containerId != null) {
			try {
				dockerClient.stopContainerCmd(containerId).exec()
			} catch (NotModifiedException e) {
				// Container already stopped. Ignore.
			}
		}
	}

	def port(int hostPort, int port = hostPort) {
		def portBinding = new DockerPortBinding(hostPort, port)
		portBindings << portBinding
	}

	ContainerInspect inspect() {
		new ContainerInspect(fullName, dockerClient.inspectContainerCmd(containerId).exec())
	}

	private String calculateFullName(String name) {
		"${appliance.name}_${name}"
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	private void createTasks() {
		createJobTask(taskName('Start'), ContainerJob.Start, this) { dependsOn prepareTask }
		createJobTask(taskName('Stop'), ContainerJob.Stop, this)
		createJobTask(taskName('Kill'), ContainerJob.Kill, this)
		createJobTask(taskName('Remove'), ContainerJob.Remove, this)
		createJobTask(taskName('Create'), ContainerJob.Create, this) { dependsOn prepareTask }
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	private String[] map2StringArray(Map<String,String> m) {
		List<String> list = m.collect { String key, String value -> "$key=$value" as String }
		list.toArray(new String[list.size()])
	}

	@Override
	String toString() {
		"Container(${fullName})"
	}
}



@Immutable
@CompileStatic
class LinkInfo {
	String name
	String alias
}
