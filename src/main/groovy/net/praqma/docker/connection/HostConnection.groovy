package net.praqma.docker.connection

import groovy.transform.CompileStatic
import groovy.transform.Immutable

import java.util.concurrent.ExecutorService

import net.praqma.gradle.docker.DockerContainer

import org.slf4j.Logger

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.NotFoundException
import com.github.dockerjava.api.command.EventCallback
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.Event
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder

@CompileStatic
class HostConnection implements EventCallback {

	/*
	 @Override
	 public Map<String, ContainerInfo> loadAll(Iterable<? extends String> keys)  {
	 logger.info "Loading container cache"
	 Collection<Container> containerList = dockerClient.listContainersCmd().withShowAll(true).exec() as Collection<Container>
	 logger.info "Loading container cache. Found ${containerList.size()} containers."
	 def m = (containerList.inject([:].withDefault{NULL_CONTAINTER_INFO}) { Map<String, ContainerInfo> m, Container c ->
	 ContainerInfo ci = new ContainerInfo(c.id, c.image)
	 c.names.collect { String n ->
	 [n[1..-1], ci]
	 }.collectEntries(m)
	 }) as Map<String, ContainerInfo>
	 m
	 }
	 */
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

	void updateContainer(DockerContainer con) {
		InspectContainerResponse icr
		try {
			icr = dockerClient.inspectContainerCmd(con.fullName).exec()
		} catch (NotFoundException e) {
			// OK. Update container from null
		}
		con.updateFrom(icr)
	}

	void start(HostSpec hostSpec, Logger logger) {
		logger.info "Starting Docker host connection"
		this.hostSpec = hostSpec
		this.logger = logger
		this.executorService = dockerClient.eventsCmd(this).exec()
	}


	void shutdown() {
		hostSpec = null
		logger = null
		executorService?.shutdown()
	}

	private DockerClient initDockerClient() {
		assert hostSpec != null
		DockerClientConfigBuilder configBuilder = DockerClientConfig.createDefaultConfigBuilder()
		hostSpec.addToClientConfigBuilder(configBuilder)
		DockerClient client = DockerClientBuilder.getInstance(configBuilder.build()).build()
		client
	}

	///////////
	//// Event handling
	///////////
		
	private ExecutorService executorService
	
	/** container id => (event => List<Action>) */
	Map<String, Map<?, List>> eventHandlers = [:].withDefault {  }
	
	void stop() {
		executorService.shutdown()
		log "Stop"
	}
	
	void register(DockerContainer container, EventName eventName, Closure closure) {
		eventHandlers[container.containerId][eventName] << closure
	}
	
	void remove(DockerContainer container) {
		eventHandlers.remove(container.containerId)
	}
	
	@Override
	void onEvent(Event event) {
		println "JHS xxxxxxxxxxxxxxxxxxxxxxxxxx"
		log event
		idCache.getIfPresent(event.id)?.dispatchEvent(event)
	}

	@Override
	void onException(Throwable throwable) {
		
	}

	@Override
	void onCompletion(int numEvents) {
		
	}
	
	private log(msg) {
		logger.info "${getClass().simpleName}: ${msg}"
	}

}

@Immutable
@CompileStatic
class ContainerInfo {
	String id
	String imageId
}
