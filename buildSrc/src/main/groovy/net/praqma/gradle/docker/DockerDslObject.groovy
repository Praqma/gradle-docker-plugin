package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.TypeCheckingMode
import net.praqma.gradle.docker.jobs.Job
import net.praqma.gradle.docker.tasks.JobBasedTask

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.Version
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl

@CompileStatic
abstract class DockerDslObject extends DockerObject {

	final String name

	DockerDslObject(String name, DockerObject parent) {
		super(parent)
		this.name = name
	}
}
