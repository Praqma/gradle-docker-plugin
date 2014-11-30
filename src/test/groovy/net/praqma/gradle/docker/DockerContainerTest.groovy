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

class DockerContainerTest extends ProjectTestCase {

	private static String BUSYBOX_IMAGE = 'busybox:latest'
	
	@Test
	void testInspectState() {
		String conName = newName('con')
		Project project = newRootProject()
		project.with {
			docker{
				container (conName) {
					persistent = true
					image BUSYBOX_IMAGE
					cmd 'sleep', '1000000'
				}
			}
		}
		DockerPluginExtension d = project.docker
		DockerContainer con = d.container(conName)
		ContainerInspect ci = con.inspect()
		assertThat ci, is(null)

		create con
		ci = con.inspect()
		assertThat ci.state, is(State.STOPPED)

		start con
		sleep 100 // Give it a chance to stop
		ci = con.inspect()
		assertThat ci.state, is(State.RUNNING)

		stop con
		ci = con.inspect()
		assertThat ci.state, is(State.STOPPED)

		remove con
		ci = con.inspect()
		assertThat ci, is(null)
	}

	@Test
	void testPersistentFlag() {
		String trueName = newName 'true'
		String falseName = newName 'false'
		String appName = newName 'app'
		Project project = newRootProject()
		project.with {
			docker{
				appliance (appName) {
					container (trueName) {
						image BUSYBOX_IMAGE
						cmd 'sleep', '10000'
						persistent = true
					}
					container (falseName) {
						image BUSYBOX_IMAGE
						cmd 'sleep', '10000'
					}
				}
			}
		}

		DockerAppliance a = project.docker.appliance appName

		DockerContainer t = a.container(trueName)
		DockerContainer f = a.container(falseName)

		assertThat t.inspect(), is(null)
		assertThat f.inspect(), is(null)

		JobScheduler.execute(ApplianceJob.Start, a)
		assertThat t.inspect().state, is(State.RUNNING)
		assertThat f.inspect().state, is(State.RUNNING)

		JobScheduler.execute(ApplianceJob.Stop, a)
		assertThat t.inspect().state, is(State.STOPPED)
		assertThat f.inspect(), is(null)
	}

	@Test
	void testVolumeBind() {
		DockerContainer w, r
		Project project = newRootProject()
		project.with {
			docker {
				w = container ('writer') {
					image BUSYBOX_IMAGE
					volume '/tmp', '/hosttmp'
					cmd 'touch', '/hosttmp/x'
				}
				r = container ('reader') {
					image BUSYBOX_IMAGE
					volume '/tmp', '/hosttmp2'
					cmd 'touch', '/hosttmp2/y'
				}
			}
		}
		start w
		start r
	}
	
	@Test
	void testLogs() {
		DockerContainer c
		projectWithDocker {
			c = container('c') {
				image BUSYBOX_IMAGE
				cmd 'sh', '-c', "echo a"
			}
			
		}
		start c
		def s = c.logStream().text.trim()
		assertThat s, is("a")
		stop c
	}
}
