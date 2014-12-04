package net.praqma.gradle.docker

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import net.praqma.gradle.docker.jobs.ApplianceJob
import net.praqma.gradle.docker.jobs.ContainerJob
import net.praqma.gradle.docker.jobs.Job
import net.praqma.gradle.docker.jobs.JobScheduler
import net.praqma.gradle.docker.test.ProjectTestCase

import org.gradle.api.Project
import org.junit.Test

class DockerApplianceTest extends ProjectTestCase {

	@Test
	void testCyclicLinks() {
		String conName = newName('con')
		Project project = projectWithDocker() {
			appliance('cyclic') {
				container ('one') {
					image BUSYBOX_IMAGE
					cmd 'sleep', 'infinity'
					link 'two', 'two'
				}
				container ('two') {
					image BUSYBOX_IMAGE
					cmd 'sleep', 'infinity'
					//link 'one', 'one'
				}

			}
		}
		start project.docker.appliance('cyclic')
	}

}
