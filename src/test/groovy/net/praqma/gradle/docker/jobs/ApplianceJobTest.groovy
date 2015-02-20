package net.praqma.gradle.docker.jobs

import net.praqma.gradle.docker.State
import org.junit.Assert;

import static org.junit.Assert.*
import net.praqma.gradle.docker.DockerAppliance
import net.praqma.gradle.docker.test.ProjectTestCase

import org.junit.Test

class ApplianceJobTest extends ProjectTestCase {

	@Test
	void testApplianceDestroy() {
		DockerAppliance app
		projectWithDocker {
			app = appliance ('test') {
				container ('c1') {
					image BUSYBOX_IMAGE
					cmd 'sleep', '10000'
				}
				container ('c2') {
					image BUSYBOX_IMAGE
					cmd 'sleep', '10000'
					persistent = true
				}
			}
		}
		JobScheduler.execute(ApplianceJob.Start, app)
        Assert.assertEquals(app.container('c1').inspect().state, State.RUNNING)
        Assert.assertEquals(app.container('c2').inspect().state, State.RUNNING)

		JobScheduler.execute(ApplianceJob.Destroy, app)
        Assert.assertNull(app.container('c1').inspect())
        Assert.assertNull(app.container('c2').inspect())
	}
	
	@Test
	void testApplianceStop() {
		DockerAppliance app
		projectWithDocker {
			app = appliance ('test') {
				container ('c1') {
					image BUSYBOX_IMAGE
					cmd 'sleep', '10000'
				}
				container ('c2') {
					image BUSYBOX_IMAGE
					cmd 'sleep', '10000'
					persistent = true
				}
			}
		}
		JobScheduler.execute(ApplianceJob.Start, app)
		// TODO assert the two containers are running
		JobScheduler.execute(ApplianceJob.Stop, app)
		// TODO assert the two containers are stopped
	}
}

