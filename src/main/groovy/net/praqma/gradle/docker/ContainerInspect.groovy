package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.Immutable

import com.github.dockerjava.api.command.InspectContainerResponse

@CompileStatic
class ContainerInspect {

	final private InspectContainerResponse icr
	final name

	ContainerInspect(String name, InspectContainerResponse icr) {
		this.name = name
		this.icr = icr
	}

	List getExposedPorts() {
		icr.config.exposedPorts as List
	}

	List<String> getVolumes() {
		icr.volumes as List
	}

	State getState() {
		def s = icr.state
		if (s.paused) return State.PAUSED
		if (s.running) return State.RUNNING
		return State.STOPPED
	}

	@Immutable
	static class Volume {

	}
}

enum State {
	STOPPED, PAUSED, RUNNING
}
