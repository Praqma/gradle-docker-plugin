package net.praqma.gradle.docker

import java.util.concurrent.CountDownLatch;

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.TypeCheckingMode
import net.praqma.docker.connection.EventName
import net.praqma.gradle.docker.jobs.ContainerJob

import org.gradle.api.GradleException
import org.gradle.api.Project

import com.github.dockerjava.api.NotFoundException
import com.github.dockerjava.api.NotModifiedException
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.command.LogContainerCmd
import com.github.dockerjava.api.command.StartContainerCmd
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Event
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Link
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.Volume


@CompileStatic
class DockerContainer extends DockerCompute {

	private String cid = '?' // null means no container id (i.e. no backing container). ? means state is unknown
	private Map<EventName, List<Closure>> eventRegistry
	private Collection<DockerPortBinding> portBindings = [] as Set
	private Collection<String> volumes = [] as Set
	private Collection<VolumeBind> volumeBinds = [] as Set
	private Collection<String> volumesFrom = [] as Set
	private Collection<LinkInfo> links = [] as Set
	private Map<String, String> env = [:]

	private ExecutionResult _executionResult

	final private RemoteDockerImage image
	String localImage
	boolean persistent = false
	String[] cmd

	@CompileDynamic
	DockerContainer(String name, CompositeCompute parent) {
		super(name, parent)
		eventRegistry = new EnumMap<EventName, List<Closure>>(EventName).withDefault { []}
		connection.updateCache(this)
		this.image = new RemoteDockerImage(this)
		if (parent.hasProperty("prepareTask")) {
			parent.prepareTask.dependsOn prepareTask
		}
	}

	@Override
	public String getTaskNamePrefix() {
		"container${fullName.capitalize()}"
	}

	String taskName(String task) {
		"${taskNamePrefix}${task.capitalize()}"
	}

	void setLocalImage(String liName) {
		this.@localImage = liName
		prepareTask.dependsOn LocalDockerImage.copyTaskName(liName)
	}

	synchronized String getContainerId() {
		if (this.@cid == '?') {
			connection.updateContainer(this, !persistent)
		}
		this.@cid
	}

	CompositeCompute getOwner() {
		parent as CompositeCompute
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

	void cmd(String...cmdArray) {
		this.cmd = cmdArray
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

	void volume(String hostPath, String containerPath) {
		this.volumeBinds << new VolumeBind(hostPath, containerPath)
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
		parent.project
	}
	
	ExecutionResult getExecutionResult() {
		this._executionResult
	}

	void create(String imageId) {
		assert imageId != null

		if (containerId == null) {
			logger.info "Creating Docker container from ${imageId}"
			CreateContainerCmd c = dockerClient.createContainerCmd(imageId)
					.withName(fullName)
					.withVolumes(volumes.collect {
						new Volume(it as String)
					} as Volume[])
					.withEnv(map2StringArray(env))
			if (cmd != null) {
				c.withCmd(cmd)
			}
			CreateContainerResponse resp = c.exec()
			assert resp.id != null
			if (resp.warnings) {
				// TODO better handling of warnings
				resp.warnings.each { println it }
			}
			this.@cid = resp.id
			connection.updateCache(this, resp.id)
		}
		this.@cid
	}

	void start() {
		logger.warn "Starting Docker container from image ${image}"
		Ports ports = new Ports()
		portBindings.each { DockerPortBinding binding ->
			ports.bind(ExposedPort.tcp(binding.containerPort), new Ports.Binding(binding.hostPort))
		}

		StartContainerCmd cmd = dockerClient.startContainerCmd(containerId)
				.withLinks(links.collect { LinkInfo li -> new Link(calculateFullName(li.name), li.alias) } as Link[])
				.withPortBindings(ports)
				.withBinds(volumeBinds.collect { VolumeBind vb -> new Bind(vb.hostPath, new Volume(vb.volume)) } as Bind[])
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

	ExecutionResult waitUntilFinish() {
		CountDownLatch latch = new CountDownLatch(1)
		whenFinish { latch.countDown() }
		latch.await()
		assert _executionResult != null
		executionResult 
	}

	InputStream logStream(LogSpec logSpec) {
		LogContainerCmd c = dockerClient.logContainerCmd(fullName)
		logSpec.applyToCmd(c)
		c.exec()
	}

	InputStream logStream(Map m) {
		logStream(m as LogSpec)
	}

	InputStream logStream() {
		logStream([:])
	}

	def kill() {
		dockerClient.killContainerCmd(fullName).exec()
	}

	def remove() {
		try {
			dockerClient.removeContainerCmd(fullName).exec()
		} catch (NotFoundException e) {
			// Ignore, container doesn't exist on host
		}
	}

	def stop() {
		try {
			dockerClient.stopContainerCmd(containerId).exec()
		} catch (NotModifiedException e) {
			// Container already stopped. Ignore.
		}
	}

	def port(int hostPort, int port = hostPort) {
		def portBinding = new DockerPortBinding(hostPort, port)
		portBindings << portBinding
	}

	ContainerInspect inspect() {
		try {
			new ContainerInspect(fullName, dockerClient.inspectContainerCmd(fullName).exec())
		} catch (NotFoundException e) {
			return null
		}
	}

	private String calculateFullName(String name) {
		"${owner.name}_${name}"
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	protected void createTasks() {
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

	void eachContainer(@DelegatesTo(DockerContainer) Closure closure) {
		closure(this)
	}

	@Override
	String toString() {
		"Container(${fullName})"
	}

	void when(String event, Closure action) {
		when(event as EventName, action)
	}

	void when(EventName event, Closure action) {
		eventRegistry[event] << action
	}

	void whenFinish(Closure closure) {
		when (EventName.die, closure)
		when (EventName.stop, closure)
		ContainerInspect ci = inspect()
		if (ci?.state == State.STOPPED) {
			assert executionResult != null
			executionResult.updateInspect(ci)
			closure()
		}
	}

	void dispatchEvent(Event event) {
		EventName e = event.status as EventName
		switch (e) {
			case EventName.die:
				_executionResult = new ExecutionResult(this, event)
				break
		}
		logger.info "Dispatching ${e} for ${this}"
		eventRegistry[e].each {
			try {
				it(this)
			} catch (Exception ex) {
				ex.printStackTrace()
			}
		}
		switch (e) {
			case EventName.destroy:
				assert this.@cid == event.id
				this.@cid = null
				break
		}
	}

	void updateFrom(InspectContainerResponse icr) {
		this.@cid = icr?.id
		connection.updateCache(this, this.@cid)
	}
}



@Immutable
@CompileStatic
class LinkInfo {
	String name
	String alias
}

@Immutable
@CompileStatic
class VolumeBind {
	String hostPath
	String volume
}

class ExecutionResult {

	private Event event
	private DockerContainer container
	private ContainerInspect _inspect
	
	ExecutionResult(DockerContainer container, Event event) {
		this.event = event
		this.container = container
	}
	
	int getExitCode() {
		inspect.exitCode
	}
	
	void updateInspect(ContainerInspect ci) {
		assert ci.state == State.STOPPED
		_inspect = ci
	}
	
	private ContainerInspect getInspect() {
		if (_inspect == null) {
			_inspect = container.inspect()
		} 
		_inspect
	}
}

