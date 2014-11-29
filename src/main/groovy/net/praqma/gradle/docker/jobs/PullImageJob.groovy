package net.praqma.gradle.docker.jobs

import groovy.transform.CompileStatic
import net.praqma.gradle.docker.RemoteDockerImage

import org.gradle.api.GradleException

@CompileStatic
class PullImageJob extends Job {

	private RemoteDockerImage image

	void init(RemoteDockerImage image) {
		this.image = image
		if (image.repository == null) {
			throw new GradleException("No repository defined for image")
		}
	}

	@Override
	public Answer doExecute() {
		logInfo 'pulling'
		String imageId = this.image.pullImage()
		if (imageId == '---retry---') {
			logInfo 'will retry'
			Answer.retry(2000)
		} else {
			logInfo 'pulled'
			Answer.success(imageId)
		}
	}

	@Override
	public Object logPrefix() {
		this.image
	}
}
