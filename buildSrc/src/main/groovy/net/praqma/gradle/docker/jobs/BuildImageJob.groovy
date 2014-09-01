package net.praqma.gradle.docker.jobs

import net.praqma.gradle.docker.LocalDockerImage

class BuildImageJob extends Job {

	private LocalDockerImage image

	void init(LocalDockerImage image) {
		this.image = image
	}

	@Override
	public Answer doExecute() {
		logInfo 'building'
		String imageId = this.image.build()
		logInfo 'built'
		Answer.success(imageId)
	}

	@Override
	public Object logPrefix() {
		this.image
	}
}
