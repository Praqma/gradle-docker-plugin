package net.praqma.gradle.docker

import net.praqma.gradle.docker.jobs.ApplianceJob
import net.praqma.gradle.docker.tasks.ApplianceInfoTask;


class DockerAppliance extends DockerDslObject implements DockerComputingTrait {

	private final NamedObjects<DockerContainer, DockerAppliance> containers

	DockerAppliance(String name, DockerPluginExtension extension) {
		super(name, extension)
		this.containers = new NamedObjects<>(this, DockerContainer)
		createTasks()
	}

	private void createTasks() {
		String namePrefix = "appliance${name.capitalize()}"
		createJobTask("${namePrefix}Start", , ApplianceJob.Start, this) { description "Start Docker applicance '${name}'" }
		createJobTask("${namePrefix}Stop", ApplianceJob.Stop, this) { description "Stop Docker applicance '${name}'" }
		createJobTask("${namePrefix}Destroy", ApplianceJob.Destroy, this) { description "Destroy Docker applicance '${name}'" }

		project.tasks.create(name: "${namePrefix}Info", type: ApplianceInfoTask) {
			group 'Docker'
			description "Info about the appliance"
			appliance owner
		}
	}

	DockerContainer container(String name, Closure configBlock = null)  {
		this.containers.get(name, configBlock)
	}

	@Override
	public void postProcess() {
		containers.postProcess()
		super.postProcess()
	}
	
}
