package net.praqma.gradle.docker.jobs

import net.praqma.gradle.docker.RemoteDockerImage

class PullImageJob extends Job {

	private RemoteDockerImage image

	void init(RemoteDockerImage image) {
		this.image = image
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
