package net.praqma.docker.connection

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.NotFoundException
import com.github.dockerjava.api.command.EventCallback
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.Event
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import net.praqma.gradle.docker.DockerContainer
import org.slf4j.Logger

import java.util.concurrent.ExecutorService

@CompileStatic
class HostConnection implements EventCallback {

    private Logger logger

    private HostSpec hostSpec

    private final Cache<String, DockerContainer> idCache
    private final Cache<String, DockerContainer> nameCache

    @Lazy
    private DockerClient dockerClient = initDockerClient()

    HostConnection() {
        CacheBuilder cb = CacheBuilder.newBuilder().weakValues()
        this.idCache = cb.build()
        this.nameCache = cb.build()
    }

    DockerClient getClient() {
        dockerClient
    }

    void updateCache(DockerContainer container, String containerId = null) {
        def key = container.fullName
        assert nameCache.getIfPresent(key) in [null, container]
        nameCache.put(key, container)
        if (containerId != null) {
            idCache.put(containerId, container)
        }
    }

    DockerContainer containerNamed(String fullName) {
        nameCache.getIfPresent(fullName)
    }

    void updateContainer(DockerContainer con, boolean remove) {
        InspectContainerResponse icr
        try {
            if (remove) {
                dockerClient.removeContainerCmd(con.dockerName).withForce(true).exec()
            } else {
                icr = dockerClient.inspectContainerCmd(con.dockerName).exec()
            }
        } catch (NotFoundException e) {
            // OK. Update container from null
        }
        con.updateFrom(icr)
    }

    void start(HostSpec hostSpec, Logger logger) {
        logger.info "Starting Docker host connection"
        this.hostSpec = hostSpec
        this.logger = logger
        assert this.executorService == null
    }


    void shutdown() {
        hostSpec = null
        logger = null
        executorService?.shutdown()
        executorService = null
    }

    private DockerClient initDockerClient() {
        assert hostSpec != null
        DockerClientConfigBuilder configBuilder = DockerClientConfig.createDefaultConfigBuilder()
        hostSpec.addToClientConfigBuilder(configBuilder)
        DockerClientConfig config = configBuilder.build()
        // If DockerCmdExeFactory isn't set, sometimes the ServiceLoader fails to find a DockerCmdExeFactory
        // The behavior seems rather non-determistic and has only been observed using the plugin (i.e. not
        // in integration test).
        DockerClient client = DockerClientBuilder.getInstance(config)
                .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl()).build()
        if (executorService == null) {
            executorService = client.eventsCmd(this).exec()
        }
        client
    }

    Collection<ContainerInfo> ps(boolean all = false) {
        dockerClient.listContainersCmd().withShowAll(all).exec().collect { Container con ->
            ContainerInfo.from(con)
        }
    }

    ///////////
    //// Event handling
    ///////////

    private ExecutorService executorService

    /** container id => (event => List<Action>) */
    Map<String, Map<EventName, List>> eventHandlers = [:].withDefault {}

    void register(DockerContainer container, EventName eventName, Closure closure) {
        eventHandlers[container.containerId][eventName] << closure
    }

    void remove(DockerContainer container) {
        eventHandlers.remove(container.containerId)
    }

    @Override
    void onEvent(Event event) {
        log "Revieved event: ${event}"
        idCache.getIfPresent(event.id)?.dispatchEvent(event)
    }

    @Override
    void onException(Throwable throwable) {
    }

    @Override
    void onCompletion(int numEvents) {
    }

    @Override
    boolean isReceiving() { true }

    private log(msg) {
        logger.info "${getClass().simpleName}: ${msg}"
    }

}

@Immutable
@CompileStatic
class ContainerInfo {
    String id
    String[] names
    String imageId

    static from(Container con) {
        new ContainerInfo(con.id, con.names, con.image)
    }
}
