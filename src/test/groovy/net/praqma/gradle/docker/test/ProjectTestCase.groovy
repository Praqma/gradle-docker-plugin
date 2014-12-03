package net.praqma.gradle.docker.test

import net.praqma.gradle.docker.DockerContainer
import net.praqma.gradle.docker.jobs.ContainerJob;
import net.praqma.gradle.docker.jobs.JobScheduler;

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import com.google.common.io.Files

class ProjectTestCase {

	static private Random rand = new Random()

	Project newRootProject(boolean withPlugin = true) {
		Project project = ProjectBuilder.builder()
				.withName("root")
				.withProjectDir(Files.createTempDir())
				.build()
		if (withPlugin) {
			project.with { apply plugin: 'net.praqma.docker' }
		}
		project
	}

	Project newProject(Project parent) {
	}

	private Project createProject(String name, File dir, Project parent) {
	}

	static String newName(String prefix) {
		"${prefix}_${rand.nextInt()}"
	}

	Project projectWithDocker(Closure c) {
		Project project = newRootProject()
		project.with {
			docker.with(c)
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

	void remove(DockerContainer c) {
		JobScheduler.execute(ContainerJob.Remove, c)
	}
	
	void waitFor(int ms, Closure cond) {
		while (!cond()) {
			if (ms > 0) {
				sleep 50
				ms -= 50
			} else {
				assert cond() : "Condition not reached in $ms milleseconds"
			}
		}
	}
}
