package net.praqma.gradle.docker.tasks

import groovy.transform.CompileStatic
import net.praqma.gradle.docker.ContainerInspect
import net.praqma.gradle.docker.DockerAppliance

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class ApplianceInfoTask extends DefaultTask {

	@Input
	DockerAppliance appliance

	@TaskAction
	void printInfo() {
		println "=== Appliance: ${appliance.name} ==="
	}

	private void printInspect(ContainerInspect inspect) {
	}
}
