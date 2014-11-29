package net.praqma.gradle.docker.test

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
			project.with {
				apply plugin: 'net.praqma.docker'
			}
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
}
