package net.praqma.docker.connection

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.gradle.api.GradleException

@CompileStatic
@ToString
class HostSpec {

    static final String DOCKER_HOST_PROPERTY_NAME = 'net.praqma.dockerHost'
    static final String DOCKER_CERT_PATH_PROPERTY_NAME = 'net.praqma.dockerCertPath'

    static final String DOCKER_HOST_ENVVAR_NAME = 'DOCKER_HOST'
    static final String DOCKER_CERT_PATH_ENVVAR_NAME = 'DOCKER_CERT_PATH'

    private URI _uri
    private File _certPath

    List<DockerHostDiscoverer> discoveres = []

    /**
     * If true and uri is not set the system will try to discover the settings for the Docker host automatically
     */
    boolean autoDiscover = true

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
        this._uri
    }

    File getCertPath() {
        this._certPath
    }

    void addToClientConfigBuilder(DockerClientConfigBuilder configBuilder) {
        String uri = uri as String
        if (uri == null) {
            throw new GradleException("URI for docker host is null")
        }
        if (uri.startsWith('tcp://')) {
            String httpProtocol = 'http' + (certPath?.path ? 's' : '')
            uri = httpProtocol + uri[3..-1]
        }

        configBuilder
                .withVersion('1.15')
                .withUri(uri)
        if (certPath?.path) {
            configBuilder.withDockerCertPath(certPath?.path)
        }
        //				.withUsername("dockeruser")
        //				.withPassword("ilovedocker")
        //				.withEmail("dockeruser@github.com")
        configBuilder.withServerAddress("https://index.docker.io/v1/")
        configBuilder
    }

    @CompileDynamic
    void initFromProjectProperties(Map<String, String> m) {
        (m[DOCKER_HOST_PROPERTY_NAME] ?: m[DOCKER_HOST_ENVVAR_NAME]).with { uri(it) }
        (m[DOCKER_CERT_PATH_PROPERTY_NAME] ?: m[DOCKER_CERT_PATH_ENVVAR_NAME])?.with { this._certPath = new File(it) }
    }

    DockerClient createClient() {
        DockerClientConfigBuilder configBuilder = DockerClientConfig.createDefaultConfigBuilder()
        addToClientConfigBuilder(configBuilder)
        DockerClientBuilder.getInstance(configBuilder.build()).build()
    }

    void discover() {
        Map<String, String> props = discoveres.findResult { it.discover() }
        if (props) {
            initFromProjectProperties(props)
        }
        if (uri == null) {
            throw new GradleException("Unable to discover a Docker host")
        }
    }

}
