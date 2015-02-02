package net.praqma.gradle.docker.jobs

import groovy.transform.CompileStatic
import net.praqma.gradle.docker.LocalDockerImage

@CompileStatic
class BuildImageJob extends Job {

	private LocalDockerImage image

	void init(LocalDockerImage image) {
		this.image = image
		if (image.baseImage) {
			preJob(BuildImageJob, image.baseImage)
		}
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
		"Image '${this.image.name}'"
	}
}
