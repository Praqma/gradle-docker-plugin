package net.praqma.gradle.docker.jobs

import net.praqma.gradle.docker.DockerAppliance
import net.praqma.gradle.docker.DockerContainer
import net.praqma.gradle.docker.State
import net.praqma.gradle.docker.test.ProjectTestCase
import org.junit.Assert
import org.junit.Test

class ApplianceJobTest extends ProjectTestCase {

    @Test
    void testApplianceDestroy() {
        DockerAppliance app
        projectWithDocker {
            app = appliance('test') {
                container('c1') {
                    image BUSYBOX_IMAGE
                    cmd 'sleep', '10000'
                }
                container('c2') {
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
        DockerContainer c1, c2
        projectWithDocker {
            app = appliance('test') {
                c1 = container('c1') {
                    image BUSYBOX_IMAGE
                    cmd 'sleep', '10000'
                }
                c2 = container('c2') {
                    image BUSYBOX_IMAGE
                    cmd 'sleep', '10000'
                    persistent = true
                }
            }
        }
        JobScheduler.execute(ApplianceJob.Start, app)
        Assert.assertEquals(c1.inspect().state, State.RUNNING)
        Assert.assertEquals(c2.inspect().state, State.RUNNING)

        JobScheduler.execute(ApplianceJob.Stop, app)
        Assert.assertNull(c1.inspect())
        Assert.assertEquals(c2.inspect().state, State.STOPPED)

        remove c2
    }
}

