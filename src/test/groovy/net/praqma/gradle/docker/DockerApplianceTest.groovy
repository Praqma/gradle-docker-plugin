package net.praqma.gradle.docker

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import net.praqma.gradle.docker.test.ProjectTestCase

import org.gradle.api.Project
import org.junit.Ignore
import org.junit.Test

class DockerApplianceTest extends ProjectTestCase {

	@Test
	@Ignore
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

	@Test
	void testTaskNames() {
		Project project = projectWithDocker() {
			appliance('abc') {
				withTasks = true
				container ('ccc') {
					withTasks = true
					image BUSYBOX_IMAGE
				}
			}
			container ('toplevel') {
				withTasks = true
				image BUSYBOX_IMAGE
			}
		}


		[
			'applianceAbcStart',
			'applianceAbcStop',
			'applianceAbcDestroy',
			'containerAbcCccStart',
			'containerAbcCccStart',
			'containerAbcCccDestroy',
			'containerToplevelStart',
			'containerToplevelStop',
			'containerToplevelDestroy'
		].each { String taskname ->
			project.tasks[taskname] // fails if task doesn't exist
		}
	}
}
