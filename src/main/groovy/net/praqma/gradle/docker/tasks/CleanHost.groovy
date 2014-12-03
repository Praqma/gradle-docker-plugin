package net.praqma.gradle.docker.tasks

import net.praqma.docker.connection.HostSpec;

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container


class CleanHost extends DefaultTask {

	@Optional
	@Input
	HostSpec host

	@Optional
	@Input
	boolean removeImages = true

	@TaskAction
	void clean() {
		if (host == null) {
			host = project.docker.host
		}

		DockerClient client = host.createClient()
		Collection<Container> containerList = client.listContainersCmd().withShowAll(true).exec() as Collection<Container>
		containerList.each { Container c ->
			client.removeContainerCmd(c.id).withForce().exec()
		}
	}
}
