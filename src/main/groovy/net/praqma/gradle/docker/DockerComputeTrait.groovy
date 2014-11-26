package net.praqma.gradle.docker

import org.gradle.api.Task



/**
 * Something that has compute resources.
 */
trait DockerComputeTrait {

	Task prepareTask

	private final List startedActions = []
	private final List stoppedActions = []

	void whenStarted(action) {
		startedActions << action
	}

	void whenStopped(action) {
		stoppedActions << action
	}

	void triggerStartedActions() {
		trigger(startedActions)
	}

	void triggerStoppedActions() {
		trigger(stoppedActions)
	}

	void trigger(List actions) {
		actions.each { it() }
	}
}
