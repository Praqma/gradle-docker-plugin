package net.praqma.gradle.docker


trait DockerComputingTrait {

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
	
	private void trigger(List actions) {
		actions.each { it() }
	}
}
