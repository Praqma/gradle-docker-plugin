package net.praqma.gradle.docker

import net.praqma.gradle.docker.jobs.ApplianceJob
import net.praqma.gradle.docker.jobs.JobScheduler
import net.praqma.gradle.docker.test.ProjectTestCase
import org.gradle.api.Project
import org.junit.Ignore
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.not

class DockerContainerTest extends ProjectTestCase {

    private static String BUSYBOX_IMAGE = 'busybox:latest'

    @Test
    void testInspectState() {
        String conName = newName('con')
        Project project = projectWithDocker {
            container(conName) {
                persistent = true
                image BUSYBOX_IMAGE
                cmd 'sleep', '1000000'
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
        Project project = projectWithDocker {
            appliance(appName) {
                container(trueName) {
                    image BUSYBOX_IMAGE
                    cmd 'sleep', '10000'
                    persistent = true
                }
                container(falseName) {
                    image BUSYBOX_IMAGE
                    cmd 'sleep', '10000'
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
        Project project = projectWithDocker {
            w = container('writer') {
                image BUSYBOX_IMAGE
                volume '/tmp', '/hosttmp'
                cmd 'touch', '/hosttmp/x'
            }
            r = container('reader') {
                image BUSYBOX_IMAGE
                volume '/tmp', '/hosttmp2'
                cmd 'touch', '/hosttmp2/y'
            }
        }
        start w
        start r
    }

    @Test
    void testLogs() {
        DockerContainer c
        projectWithDocker {
            c = container('con') {
                image BUSYBOX_IMAGE
                cmd 'sh', '-c', "echo a"
            }
        }
        start c
        sleep 500
        def s = c.logStream().text.trim()
        assertThat s, is("a")
        stop c
    }

    @Test
    void testWhenFinish() {
        DockerContainer c1, c2
        projectWithDocker {
            c1 = container('c1') {
                image BUSYBOX_IMAGE
                cmd 'sh', '-c', "exit 0"
            }
            c2 = container('c2') {
                image BUSYBOX_IMAGE
                cmd 'sh', '-c', "exit 0"
                persistent = true
            }
        }
        def flag1, flag2
        start c1
        start c2
        c1.whenFinish { flag1 = true }
        c2.whenFinish { flag2 = true }

        waitFor(1000) {
            flag1 == true && flag2 == true
        }

        assertThat flag1, is(true)
        assertThat flag2, is(true)
    }

    @Test
    void testWaitUntilFinish() {
        DockerContainer c1, c2
        projectWithDocker {
            c1 = container('c1') {
                image BUSYBOX_IMAGE
                cmd 'sh', '-c', "sleep 1 ; exit 1"
            }
            c2 = container('c2') {
                image BUSYBOX_IMAGE
                cmd 'sh', '-c', "exit 2"
            }
        }
        start c1
        start c2
        ExecutionResult result1 = c1.waitUntilFinish()
        assertThat result1.exitCode, is(1)

        ExecutionResult result2 = c2.waitUntilFinish()
        assertThat result2.exitCode, is(2)
    }

    @Test
    @Ignore
    // Starting container with Job doesn't execute prepare task, hench dockerfile is not created. Fix it
    void testStartingRunningContainer() {
        DockerContainer c

        projectWithDocker {
            image('image') {
                dockerFile {
                    fromImage BUSYBOX_IMAGE
                    run 'sh', '-c', 'date > /date'
                }
            }
            c = container('con') {
                localImage = 'image'
                cmd 'sh', '-c', 'cat /date ; ls /etc/debian_version' // no such file on busybox
            }
        }
        JobScheduler scheduler = new JobScheduler()
        scheduler.launch(c.startJob(scheduler))
        assertThat c.waitUntilFinish().exitCode, is(not(0))
        String date1 = c.logStream().text

        projectWithDocker {
            image('image') {
                dockerFile {
                    fromImage BUSYBOX_IMAGE
                    run 'sh', '-c', 'date > /date'
                }
            }
            c = container('con') {
                localImage = 'image'
                cmd 'sh', '-c', 'cat /date ; ls /etc/debian_version' // no such file on busybox
            }
        }
        scheduler = new JobScheduler()
        scheduler.launch(c.startJob(scheduler))
        assertThat c.waitUntilFinish().exitCode, is(1)
        String date2 = c.logStream().text

        assertThat date1, is(date2)


        projectWithDocker {
            c = container('con') {
                image 'debian:wheezy'
                cmd 'ls', '/date'
            }
        }
        scheduler = new JobScheduler()
        scheduler.launch(c.startJob(scheduler))
        assertThat c.waitUntilFinish().exitCode, is(0)
    }
}
