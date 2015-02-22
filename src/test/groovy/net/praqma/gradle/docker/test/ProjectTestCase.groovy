package net.praqma.gradle.docker.test

import com.google.common.io.Files
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.praqma.gradle.docker.DockerAppliance
import net.praqma.gradle.docker.DockerContainer
import net.praqma.gradle.docker.jobs.ApplianceJob
import net.praqma.gradle.docker.jobs.ContainerJob
import net.praqma.gradle.docker.jobs.JobScheduler
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After

@CompileStatic
class ProjectTestCase {

    static String BUSYBOX_IMAGE = 'busybox:buildroot-2014.02'

    private List<Project> projectsCreated = []

    Project newRootProject(boolean withPlugin = true) {
        Project project = ProjectBuilder.builder()
                .withName("root")
                .withProjectDir(Files.createTempDir())
                .build()
        if (withPlugin) {
            project.with { apply plugin: 'net.praqma.docker' }
        }
        projectsCreated << project
        project
    }

    Project newProject(Project parent) {
    }

    private Project createProject(String name, File dir, Project parent) {
    }

    @CompileDynamic
    Project projectWithDocker(Closure c) {
        Project project = newRootProject()
        project.with {
            docker.with(c)
            docker.startDockerConnection()
        }
        project
    }

    void create(DockerContainer c) {
        JobScheduler.execute(ContainerJob.Create, c)
    }

    void start(DockerContainer c) {
        JobScheduler.execute(ContainerJob.Start, c)
    }

    void stop(DockerContainer c) {
        JobScheduler.execute(ContainerJob.Stop, c)
    }

    void remove(DockerContainer ...containers) {
        containers.each { DockerContainer c ->
            JobScheduler.execute(ContainerJob.Remove, c)
        }
    }

    void start(DockerAppliance app) {
        JobScheduler.execute(ApplianceJob.Start, app)
    }

    void stop(DockerAppliance app) {
        JobScheduler.execute(ApplianceJob.Stop, app)
    }

    void waitFor(int ms, Closure cond) {
        int delay = 0
        while (!cond()) {
            if (delay < ms) {
                sleep 50
                delay += 50
            } else {
                assert cond(): "Condition not reached in $ms milleseconds"
            }
        }
    }

    @After
    @CompileDynamic
    void tearDown() {
        projectsCreated.each {
            if (it.hasProperty('docker')) {
                it.docker.connection.shutdown()
            }
        }
    }
}
