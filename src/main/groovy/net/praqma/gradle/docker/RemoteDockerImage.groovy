package net.praqma.gradle.docker

import net.praqma.gradle.docker.json.JsonObjectExtractor
import net.praqma.gradle.utils.ProgressReporter

import org.gradle.api.GradleException


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
		if (repository == null) {
		assert repository != null
		}	
		InputStream stream = dockerClient.pullImageCmd(repository).withTag(tag).exec() as InputStream
		def lastStatus
		def maps = []
		ProgressReporter.evaluate(project, desc) { ProgressReporter reporter ->
			new JsonObjectExtractor(stream).each { Map m ->
				if (m['error']) {
					String error = m['error']
					println error
					throw new GradleException(error)
				}
				if (m['id']) id = m['id']
				String status = m['status']
				String progress = m['progress']
				maps << m
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
		if (lastStatus.endsWith('already being pulled by another client. Waiting.')) {
			return "---retry---"
		}
		if (id == null) {
			maps.each { println it }
		}
		assert id != null
		id
	}

	@Override
	public String toString() {
		"${repository}:${tag}"
	}
}
