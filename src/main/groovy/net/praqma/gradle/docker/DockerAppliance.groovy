package net.praqma.gradle.docker

import net.praqma.gradle.docker.tasks.ApplianceInfoTask

class DockerAppliance extends DockerCompute implements CompositeCompute {

	DockerAppliance(String name, DockerPluginExtension extension) {
		super(name, extension, ComputeDescriptor.appliance)
		initCompositeCompute(this)
	}
	
	String getTaskNamePrefix() {
		"appliance${name.capitalize()}"
	}

	protected void createTasks() {
		super.createTasks()
		project.tasks.with {
			create(name: "${taskNamePrefix}Info", type: ApplianceInfoTask) {
				group 'Docker'
				description "Info about the appliance"
				appliance this
			}
		}
	}

	@Override
	String toString() {
		"Appliance(${name})"
	}
	
}
