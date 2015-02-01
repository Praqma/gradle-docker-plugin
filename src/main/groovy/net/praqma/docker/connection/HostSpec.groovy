package net.praqma.docker.connection

import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic
import groovy.transform.ToString

import org.gradle.api.GradleException
import org.gradle.api.Project

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder

@CompileStatic
@ToString
class HostSpec {

	private URI _uri
	private File _certPath

	String version = '1.16'

	def uri(String s) {
		_uri = URI.create(s)
	}

	def certPath(File certPath) {
		this._certPath = certPath
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
		if (this._uri == null) {
			String s = System.getenv('DOCKER_HOST')
			if (s == null) return null
			if (s.startsWith('tcp://')) {
				s = 'https' + s[3..-1]
			}
			uri(s)
		}
		this._uri
	}

	File getCertPath() {
		if (this._certPath == null) {
			String s = System.getenv('DOCKER_CERT_PATH')
			this._certPath = s ? new File(s) : null
		}
		this._certPath
	}

	void addToClientConfigBuilder(DockerClientConfigBuilder configBuilder) {
		String uri = uri as String
		if (uri == null) {
			throw new GradleException("URI for docker host is null")
		}
		configBuilder
				.withVersion(version)
				.withUri(uri)
				//				.withUsername("dockeruser")
				//				.withPassword("ilovedocker")
				//				.withEmail("dockeruser@github.com")
				.withServerAddress("https://xxxxindex.docker.io/v1/")
				.withDockerCertPath(certPath.path)
		configBuilder
	}

	@CompileDynamic
	void initFromProjectProperties(Map<String,String> properies) {
		properties.dockerCertPath?.with {
			this._certPath = new File(it)
		}
		properties.dockerHost?.with { uri(it) }
	}

	DockerClient createClient() {
		DockerClientConfigBuilder configBuilder = DockerClientConfig.createDefaultConfigBuilder()
		addToClientConfigBuilder(configBuilder)
		DockerClientBuilder.getInstance(configBuilder.build()).build()
	}
}
