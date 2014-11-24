package net.praqma.gradle.docker.tasks

import net.praqma.gradle.docker.LocalDockerImage

import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskAction

class BuildImageTask extends Copy {

	LocalDockerImage dockerImage

	void setDockerImage(LocalDockerImage image) {
		this.dockerImage = image
		inputs.dir image.directory
	}

	@TaskAction
	void buildImage() {
		dockerImage.build()
	}
}
