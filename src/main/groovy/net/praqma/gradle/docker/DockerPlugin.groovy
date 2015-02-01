package net.praqma.gradle.docker

import net.praqma.gradle.docker.tasks.CleanHost

import org.gradle.api.Plugin
import org.gradle.api.Project


class DockerPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.apply(plugin: 'base')
		project.extensions.create('docker', DockerPluginExtension, project)
		
		project.task("cleanDockerHost", type: CleanHost) {
			group 'docker'
			description "Remote all containers from the docker host"
		}

		project.task("sweepDockerHost", type: CleanHost) {
			group 'docker'
			description "Remove all containers and images from the docker host"
		}
		
	}

}
