package net.praqma.gradle.docker

import net.praqma.gradle.docker.jobs.ApplianceJob
import net.praqma.gradle.docker.tasks.ApplianceInfoTask;


class DockerAppliance extends DockerDslObject {

	private final NamedObjects<DockerContainer, DockerAppliance> containers

	DockerAppliance(String name, DockerPluginExtension extension) {
		super(name, extension)
		this.containers = new NamedObjects<>(this, DockerContainer)
		createTasks()
	}

	private void createTasks() {
		createJobTask("${name}Start", , ApplianceJob.Start, this) { description "Start Docker applicance '${name}'" }
		createJobTask("${name}Stop", ApplianceJob.Stop, this) { description "Stop Docker applicance '${name}'" }
		createJobTask("${name}Destroy", ApplianceJob.Destroy, this) { description "Destroy Docker applicance '${name}'" }

		project.tasks.create(name: "${name}Info", type: ApplianceInfoTask) {
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
