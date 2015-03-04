package net.praqma.gradle.docker

import com.github.dockerjava.api.NotFoundException
import com.github.dockerjava.api.NotModifiedException
import com.github.dockerjava.api.command.*
import com.github.dockerjava.api.model.*
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.TypeCheckingMode
import net.praqma.docker.connection.EventName
import net.praqma.gradle.docker.VolumeBind
import net.praqma.gradle.docker.jobs.ContainerJob
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task

import java.util.concurrent.CountDownLatch

@CompileStatic
class DockerContainer extends DockerCompute {

    private String cid = '?' // null means no container id (i.e. no backing container). ? means state is unknown
    private String imageId // Image id that backing container is base upon
    private Map<EventName, List<Closure>> eventRegistry
    private Collection<DockerPortBinding> portBindings = [] as Set
    private Collection<String> volumes = [] as Set
    private Collection<VolumeBind> volumeBinds = [] as Set
    private Collection volumesFrom = [] as Set
    private Collection<LinkInfo> links = [] as Set
    private Map<String, String> env = [:]

    private ExecutionResult _executionResult

    final private RemoteDockerImage image
    String localImage
    boolean persistent = false
    String[] cmd

    @Lazy
    Task createTask = createCreateTask()
    @Lazy
    Task runTask = createRunTask()

    int stopTimeoutSeconds = 5

    DockerContainer(String name, CompositeCompute parent) {
        super(name, parent, ComputeDescriptor.container)
        eventRegistry = new EnumMap<EventName, List<Closure>>(EventName).withDefault { [] }
        connection.updateCache(this)
        this.image = new RemoteDockerImage(this)
        if (parent.hasProperty("prepareTask")) {
            (parent.properties['prepareTask'] as Task).dependsOn prepareTask
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

    void cmd(String... cmdArray) {
        this.cmd = cmdArray
    }

    String getFullName() {
        if (owner instanceof DockerAppliance) {
            "${owner.name}${name.capitalize()}"
        } else {
            name
        }
    }

    /**
     * @return name of underlying docker container
     */
    String getDockerName() {
        if (owner instanceof DockerAppliance) {
            "${owner.name}_${name}"
        } else {
            name
        }
    }

    void env(Map<String, String> m) {
        this.env.putAll(m)
    }

    void volume(String volume) {
        this.volumes << volume
    }

    void volume(String hostPath, String containerPath) {
        this.volumeBinds << new VolumeBind(hostPath, containerPath)
    }

    void volume(File hostPath, String containerPath) {
        volume(hostPath.absolutePath, containerPath)
    }

    void volumesFrom(DockerContainer container) {
        volumesFrom << container
    }

    void volumesFrom(String name) {
        volumesFrom << name
    }

    Collection<DockerContainer> getVolumesFromContainers() {
        volumesFrom.collect {
            switch (it) {
                case String:
                    this.owner.container(it as String)
                    break
                case DockerContainer:
                    it
                    break
                default:
                    throw new GradleException("Unexpected value: ${it}")
            }
        }
    }

    void link(DockerContainer container, String alias = null) {
        links << new LinkInfo(container, null, alias)
    }

    void link(String containerName, String alias = containerName) {
        links << new LinkInfo(null, containerName, alias)
    }

    Project getProject() {
        parent.project
    }

    ExecutionResult getExecutionResult() {
        this._executionResult
    }

    void create(String imageId) {
        assert imageId != null

        if (containerId == null || this.imageId != imageId) {
            logger.info "Creating Docker container from ${imageId}"
            CreateContainerCmd c = dockerClient.createContainerCmd(imageId)
                    .withName(dockerName)
                    .withTty(true)
                    // TODO Shouldn't it be possible to set VolumeBinds
                    .withVolumes(volumes.collect { new Volume(it as String) } as Volume[])
                    .withEnv(map2StringArray(env))
            if (cmd != null) {
                c.withCmd(cmd)
            }
            CreateContainerResponse resp = c.exec()
            assert resp.id != null
            if (resp.warnings) {
                resp.warnings.each { project.logger.warn(it as String) }
            }
            this.@cid = resp.id
            connection.updateCache(this, resp.id)
            this.imageId = imageId
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
                .withLinks(links.collect { LinkInfo li -> li.asLink(this.owner) } as Link[])
                .withPortBindings(ports)
                .withBinds(volumeBinds.collect { VolumeBind vb -> new Bind(vb.hostPath, new Volume(vb.volume)) } as Bind[])
        // TODO set more volumes
        if (this.volumesFrom.size() > 0) {
            cmd.withVolumesFrom(volumesFromContainers.first().dockerName)
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
        LogContainerCmd c = dockerClient.logContainerCmd(dockerName)
        logSpec.applyToCmd(c)
        c.exec()
    }

    InputStream logStream(Map m) {
        logStream(m as LogSpec)
    }

    InputStream logStream() {
        logStream([:])
    }

    def remove() {
        try {
            dockerClient.removeContainerCmd(dockerName).exec()
        } catch (NotFoundException e) {
            // Ignore, container doesn't exist on host
        }
    }

    def stop(int timeout = stopTimeoutSeconds) {
        if (containerId != null) {
            try {
                dockerClient.stopContainerCmd(containerId).withTimeout(timeout).exec()
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
        try {
            new ContainerInspect(fullName, dockerClient.inspectContainerCmd(dockerName).exec())
        } catch (NotFoundException e) {
            return null
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void createTasks() {
        super.createTasks()
        // lazy created
        createTask
        runTask
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private String[] map2StringArray(Map<String, String> m) {
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
        when(EventName.die, closure)
        when(EventName.stop, closure)
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

    @CompileDynamic
    private Task createCreateTask() {
        createJobTask(taskName('Create'), ContainerJob.Create, this) {
            description "Create ${computeDescription}"
            dependsOn prepareTask
        }
    }

    @CompileDynamic
    private Task createRunTask() {
        project.tasks.create(taskName('Run')) {
            group 'docker'
            description "Run ${computeDescription} (i.e. start it and wait for it to finish)"
            dependsOn startTask

            doLast {
                ExecutionResult er = waitUntilFinish()
                if (er.exitCode != 0) {
                    throw new GradleException("Container exit code: ${er.exitCode}")
                }
            }
        }
    }
}


@Immutable
@CompileStatic
class LinkInfo {
    DockerContainer container
    String name
    String alias

    Link asLink(CompositeCompute owner) {
        DockerContainer c = container ?: owner.container(name)
        new Link(c.dockerName, alias)
    }
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

