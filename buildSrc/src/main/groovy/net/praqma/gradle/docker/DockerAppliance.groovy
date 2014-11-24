package net.praqma.gradle.docker

import groovy.transform.ToString;
import net.praqma.gradle.docker.jobs.ApplianceJob
import net.praqma.gradle.docker.tasks.ApplianceInfoTask

import org.gradle.api.Task


@ToString(includes = 'name')
class DockerAppliance extends DockerDslObject implements DockerComputeTrait {

	private final NamedObjects<DockerContainer, DockerAppliance> containers

	DockerAppliance(String name, DockerPluginExtension extension) {
		super(name, extension)
		this.containers = new NamedObjects<>(this, DockerContainer)
		createTasks()
	}

	private void createTasks() {
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

	DockerContainer container(String name, Closure configBlock = null)  {
		this.containers.getObject(name, configBlock)
	}

	@Override
	void postProcess() {
		containers.postProcess()
		super.postProcess()
	}
	@Override
	String toString() {
		"Appliance(${name})"
	}

	
}
