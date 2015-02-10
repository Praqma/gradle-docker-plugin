package net.praqma.gradle.docker

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.praqma.gradle.docker.jobs.Job
import net.praqma.gradle.docker.jobs.JobScheduler

import org.gradle.api.Task



/**
 * Something that has compute resources.
 * <p>
 * I.e. container or appliances
 * 
 */
@CompileStatic
abstract class DockerCompute extends DockerDslObject {

	Task prepareTask

	@Lazy Task startTask = createStartTask()
	@Lazy Task stopTask = createStopTask()
	@Lazy Task destroyTask = createDestroyTask()
	
	boolean withTasks = false

	private final List startedActions = []
	private final List stoppedActions = []

	private final ComputeDescriptor descriptor

	DockerCompute(String name, CompositeCompute parent, ComputeDescriptor descriptor) {
		super(name, parent as DockerObject)
		assert parent != null
		this.descriptor = descriptor
		prepareTask = project.tasks.create(name: "${taskNamePrefix}Prepare")
	}

	String getComputeDescription() {
		"${descriptor.description(this)}"
	}
	abstract String getTaskNamePrefix()

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

	@CompileDynamic
	void trigger(List actions) {
		actions.each { it() }
	}

	Job startJob(JobScheduler scheduler) {
		Job j = scheduler.newJob(descriptor.jobStartClass, this)
		j
	}

	Job stopJob(JobScheduler scheduler) {
		scheduler.newJob(descriptor.jobStopClass, this)
	}

	@CompileDynamic
	Task createStartTask() {
		createJobTask("${taskNamePrefix}Start", descriptor.jobStartClass, this) {
			description "Start ${computeDescription}"
			dependsOn prepareTask
		}
	}

	@CompileDynamic
	Task createStopTask() {
		createJobTask("${taskNamePrefix}Stop", descriptor.jobStopClass, this) { description "Stop ${computeDescription}" }
	}

	@CompileDynamic
	Task createDestroyTask() {
		createJobTask("${taskNamePrefix}Destroy", descriptor.jobDestroyClass, this) { description "Destroy ${computeDescription}" }
	}

	protected void createTasks() {
		// Tasks are created lazy and referencing them will create them
		startTask
		stopTask
		destroyTask
	}
}
