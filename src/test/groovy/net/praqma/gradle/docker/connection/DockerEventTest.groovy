package net.praqma.gradle.docker.connection

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import net.praqma.gradle.docker.DockerContainer
import net.praqma.gradle.docker.test.ProjectTestCase

import org.junit.Test

class DockerEventTest extends ProjectTestCase{

	@Test
	void test() {
		DockerContainer c
		def events = []
		projectWithDocker {
			c = container ('con') {
				image 'busybox:latest'
				cmd 'sleep', '0.001'
				
				when ('start') { 
					events << 'start'
				}
				when ('die') { 
					events << 'die' 
				}
				when ('stop') {
					events << 'stop'
				}
			}
			
		}
		start c
		waitFor(1000) {
			events.size() == 2
		}
		assertThat events, is(['start', 'die'])
		stop c
	}
}
