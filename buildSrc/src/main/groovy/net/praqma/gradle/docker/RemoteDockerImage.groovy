package net.praqma.gradle.docker

import net.praqma.gradle.docker.json.JsonObjectExtractor
import net.praqma.gradle.utils.ProgressReporter


class RemoteDockerImage extends DockerObject {

	String repository
	String tag = 'latest'

	RemoteDockerImage(DockerObject parent) {
		super(parent)
	}

	String pullImage() {
		String id
		def desc = "Pulling Docker repository ${repository}:${tag}"
		logger.warn desc
		InputStream stream = dockerClient.pullImageCmd(repository).withTag(tag).exec() as InputStream
		def lastStatus
		ProgressReporter.evaluate(project, desc) { ProgressReporter reporter ->
			new JsonObjectExtractor(stream).each { Map m ->
				id = m['id']
				String status = m['status']
				String progress = m['progress']
				String msg
				if (progress) {
					msg = "${status}: ${progress}"
				} else {
					msg = status
				}
				reporter.update(msg)
				if (status != null) lastStatus = status
			}
		}
		if (lastStatus == 'Repository mongo already being pulled by another client. Waiting.') {
			return "---retry---"
		}
		assert id != null
		id
	}

	@Override
	public String toString() {
		"${repository}:${tag}"
	}
}
