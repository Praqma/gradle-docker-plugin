package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import net.praqma.docker.connection.HostConnection
import net.praqma.gradle.docker.jobs.Job
import net.praqma.gradle.docker.tasks.JobBasedTask

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container

@CompileStatic
abstract class DockerObject {

	final DockerObject parent

	DockerObject(DockerObject parent) {
		this.@parent = parent
	}

	HostConnection getConnection() {
		parent.connection
	}

	DockerClient getDockerClient() {
		connection.client
	}

	Project getProject() {
		parent.project
	}

	DockerPluginExtension getDockerExtension() {
		parent ? parent.dockerExtension : (this as DockerPluginExtension)
	}

	void evalClosureWithDelegate(delegate = null, Closure closure) {
		if (closure) {
			closure.delegate = delegate ?: this
			closure.resolveStrategy = Closure.DELEGATE_FIRST
			closure()
		}
	}

	/** Create a Gradle task, implemeted by executing a Job */
	@CompileStatic(TypeCheckingMode.SKIP)
	Task createJobTask(String name, Class<? extends Job> jobCls, Object ...arguments) {
		Closure configBlock = null
		Task task = project.tasks.create(name: name, type: JobBasedTask) {
			if (arguments.size() > 0 && arguments[-1] instanceof Closure) {
				configBlock = arguments[-1]
				arguments = arguments[0..-2]
			}
			group 'docker'
			jobClass jobCls
			args arguments
		}
		assert task != null
		if (configBlock) task.configure configBlock
		task
	}

	Map<String, String> dockerVersion() {
		dockerClient.versionCmd().exec().properties
	}

	Logger getLogger() {
		project.logger
	}

}
