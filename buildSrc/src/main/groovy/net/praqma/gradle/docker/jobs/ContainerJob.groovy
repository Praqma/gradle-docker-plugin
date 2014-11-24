package net.praqma.gradle.docker.jobs

import groovy.transform.CompileStatic
import net.praqma.gradle.docker.ContainerInfo
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
		"Container '${container.name}' in '${container.appliance.name}'"
	}

	static class Start extends ContainerJob {

		@Override
		public void init(DockerContainer container) {
			super.init(container)
			preJob(Create, container)
			container.links.each { LinkInfo li ->
				DockerContainer linkContainer = container.appliance.container(li.name)
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
			Answer.success()
		}
	}

	static class Create extends ContainerJob {

		private Job getImageIdJob

		@Override
		public void init(DockerContainer container) {
			super.init(container)
			ContainerInfo info = container.findContainerByName(container.fullName)
			if (info != null) {
				String id = info.getId()
				container.containerId = id
				complete(id)
			} else {
				if (container.localImage != null) {
					LocalDockerImage image = container.dockerExtension.image(container.localImage)
					getImageIdJob = preJob(BuildImageJob, image)
				} else {
					getImageIdJob = preJob(PullImageJob, container.image)
				}
				container.volumesFrom.each { String volumeFrom ->
					DockerContainer volumeFromContainer = container.appliance.container(volumeFrom)
					preJob(Create, volumeFromContainer)
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


	static class Kill extends ContainerJob {

		@Override
		public Answer doExecute() {
			if (container.containerId == null) {
				logInfo 'asked to kill, but no underlying container'
			} else {
				logInfo "killing, id: ${container.containerId}"
				container.kill()
				logInfo 'killed'
			}
			Answer.success()
		}
	}

	static class Remove extends ContainerJob {

		@Override
		public Answer doExecute() {
			if (container.containerId == null) {
				logInfo 'asked to remove, but no underlying container'
			} else {
				logInfo "removing, id: ${container.containerId}"
				container.remove()
				logInfo 'removed'
			}
			Answer.success()
		}
	}
}
