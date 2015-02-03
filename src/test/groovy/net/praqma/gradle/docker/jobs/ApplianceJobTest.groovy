package net.praqma.gradle.docker.jobs;

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
		// TODO assert the two containers are running
		JobScheduler.execute(ApplianceJob.Destroy, app)
		// TODO assert the two containers are destroyed
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

