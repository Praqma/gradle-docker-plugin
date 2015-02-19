package net.praqma.gradle.docker.jobs

import groovy.transform.CompileStatic
import net.praqma.gradle.docker.DockerContainer
import net.praqma.gradle.docker.LinkInfo
import net.praqma.gradle.docker.LocalDockerImage

@CompileStatic
abstract class ContainerJob extends Job {

	DockerContainer container

	void init(DockerContainer container) {
		this.container = container
	}

	@Override
	def logPrefix() {
		"Container '${container.name}' in '${container.owner.name}'"
	}

	static class Start extends ContainerJob {

		@Override
		public void init(DockerContainer container) {
			super.init(container)
			preJob(Create, container)
			container.links.each { LinkInfo li ->
				DockerContainer linkContainer = container.owner.container(li.name)
				preJob(Start, linkContainer)
			}
		}


		@Override
		public Answer doExecute() {
			logInfo 'starting'
			container.start()
			logInfo 'started'
			Answer.success()
		}
	}

	static class Stop extends ContainerJob {

		@Override
		public Answer doExecute() {
			logInfo 'stopping'
			container.stop()
			logInfo 'stopped'
			if (!container.persistent) {
				container.remove()
				logInfo 'removed'
			}
			Answer.success()
		}
	}

	static class Create extends ContainerJob {

		private Job getImageIdJob

		@Override
		public void init(DockerContainer container) {
			super.init(container)
			if (container.containerId != null) {
				complete(container.containerId)
			} else {
				if (container.localImage != null) {
					LocalDockerImage image = container.dockerExtension.image(container.localImage)
					getImageIdJob = preJob(BuildImageJob, image)
				} else {
					getImageIdJob = preJob(PullImageJob, container.image)
				}
				container.volumesFromContainers.each { DockerContainer con ->
					preJob(Create, con)
				}
			}
		}

		@Override
		public Answer doExecute() {
			logInfo 'creating'
			String containerId = container.create(getImageIdJob.get() as String)
			logInfo 'created with id ' + containerId
			Answer.success(containerId)
		}
	}

	static class Remove extends ContainerJob {

		@Override
		public Answer doExecute() {
			logInfo "removing, id: ${container.fullName}"
			container.remove()
			logInfo 'removed'
			Answer.success()
		}
	}
}
