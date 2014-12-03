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
			String name = newName('con')
			c = container (name) {
				image 'busybox:latest'
				cmd 'sleep', 'infinity'
				
				when ('start') { 
					events << 'start'
				}
				when ('die') { 
					events << 'die' 
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
