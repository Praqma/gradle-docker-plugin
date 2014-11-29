package net.praqma.gradle.docker

import org.gradle.api.Task



/**
 * Something that has compute resources.
 * <p>
 * I.e. container or appliances
 * 
 */
abstract class DockerCompute extends DockerDslObject {

	Task prepareTask
	boolean withTasks = false

	private final List startedActions = []
	private final List stoppedActions = []

	DockerCompute(String name, CompositeCompute parent) {
		super(name, parent)
		assert parent != null
	}

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

	protected void postProcess() {
		if (withTasks) {
			createTasks()
		}
	}

	protected void createTasks() {}
}
