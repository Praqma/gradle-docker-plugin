package net.praqma.gradle.docker

import groovy.lang.Closure;
import groovy.transform.ToString;
import net.praqma.gradle.docker.jobs.ApplianceJob
import net.praqma.gradle.docker.tasks.ApplianceInfoTask

import org.gradle.api.Task

class DockerAppliance extends DockerCompute implements CompositeCompute {

	DockerAppliance(String name, DockerPluginExtension extension) {
		super(name, extension)
		initCompositeCompute(this)
	}

	protected void createTasks() {
		String namePrefix = "appliance${name.capitalize()}"

		project.tasks.with {
			create(name: "${namePrefix}Info", type: ApplianceInfoTask) {
				group 'Docker'
				description "Info about the appliance"
				appliance this
			}
			prepareTask = create(name: "${namePrefix}Prepare")
		}
		createJobTask("${namePrefix}Start", , ApplianceJob.Start, this) {
			description "Start Docker applicance '${name}'"
			dependsOn prepareTask
		}
		createJobTask("${namePrefix}Stop", ApplianceJob.Stop, this) { description "Stop Docker applicance '${name}'" }
		createJobTask("${namePrefix}Destroy", ApplianceJob.Destroy, this) { description "Destroy Docker applicance '${name}'" }
	}

	@Override
	String toString() {
		"Appliance(${name})"
	}

	
}
