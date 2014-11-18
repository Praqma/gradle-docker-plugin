package net.praqma.gradle.docker

import groovy.transform.CompileStatic

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder

@CompileStatic
class DockerHost {
	private URI uri

	String version = '1.15'

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
			if (s == null) return null
			if (s.startsWith('tcp://')) {
				s = 'https' + s[3..-1]
			}
			this.uri = URI.create(s)
		}
		this.uri
	}

	File getCertPath() {
		// TODO
		new File("/Users/jan/.boot2docker/certs/boot2docker-vm")
	}

	void addToClientConfigBuilder(DockerClientConfigBuilder configBuilder) {
		configBuilder
				.withVersion(version)
				.withUri(getUri() as String)
				//				.withUsername("dockeruser")
				//				.withPassword("ilovedocker")
				//				.withEmail("dockeruser@github.com")
				// .withServerAddress("https://index.docker.io/v1/")
				.withDockerCertPath(certPath.path)
		configBuilder
	}

	DockerClient createClient() {
		DockerClientConfigBuilder configBuilder = DockerClientConfig.createDefaultConfigBuilder()
		addToClientConfigBuilder(configBuilder)
		DockerClientBuilder.getInstance(configBuilder.build()).build()
	}
}
