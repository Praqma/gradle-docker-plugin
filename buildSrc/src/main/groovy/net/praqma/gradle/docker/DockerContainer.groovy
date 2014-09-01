package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.TypeCheckingMode
import net.praqma.gradle.docker.jobs.ContainerJob

import org.gradle.api.Project

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.StartContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Link
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.Volume


@CompileStatic
class DockerContainer extends DockerDslObject {

	private String containerId
	private Collection<DockerPortBinding> portBindings = [] as Set
	private Collection<String> volumes = [] as Set
	private Collection<String> volumesFrom = [] as Set
	private Collection<LinkInfo> links = [] as Set

	final private RemoteDockerImage image
	String localImage

	@CompileStatic(TypeCheckingMode.SKIP)
	DockerContainer(String name, DockerAppliance appliance) {
		super(name, appliance)
		this.image = new RemoteDockerImage(this)
		createTasks()
	}

	DockerAppliance getAppliance() {
		parent as DockerAppliance
	}

	void image(Closure closure) {
		this.image.with closure
	}

	String getFullName() {
		calculateFullName(name)
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
		cmd.exec()
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
			dockerClient.stopContainerCmd(containerId).exec()
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
		createJobTask("dockerContainerStart_${fullName}" as String, ContainerJob.Start, this)
		createJobTask("dockerContainerStop_${fullName}" as String, ContainerJob.Stop, this)
		createJobTask("dockerContainerKill_${fullName}" as String, ContainerJob.Kill, this)
		createJobTask("dockerContainerRemove_${fullName}" as String, ContainerJob.Remove, this)
		createJobTask("dockerContainerCreate_${fullName}" as String, ContainerJob.Create, this)
	}

}



@Immutable
@CompileStatic
class LinkInfo {
	String name
	String alias
}
