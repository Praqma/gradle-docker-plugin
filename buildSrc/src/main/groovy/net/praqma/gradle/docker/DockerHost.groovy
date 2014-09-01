package net.praqma.gradle.docker

import groovy.transform.CompileStatic

@CompileStatic
class DockerHost {
	private URI uri

	String version = '1.14'

	def uri(String s) {
		uri = URI.create(s)
	}

	def getScheme() {
		uri.scheme
	}

	def getHost() {
		uri.host
	}

	def getPort() {
		uri.port
	}

	URI getUri() {
		if (this.uri == null) {
			String s = System.getProperty('net.praqma.dockerHost') ?: System.getenv()['DOCKER_HOST']
			if (s == null) {
				File file = new File(System.getProperty("user.home"), '.docker_host')
				if (file.exists()) s = file.text.trim()
			}
			if (s == null) return null
			if (s.startsWith('tcp://')) {
				s = 'http' + s[3..-1]
			}
			this.uri = URI.create(s)
		}
		this.uri
	}
}
