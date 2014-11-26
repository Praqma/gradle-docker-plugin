package net.praqma.gradle.docker.jobs

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import net.praqma.gradle.docker.DockerAppliance
import net.praqma.gradle.docker.DockerContainer

@CompileStatic
abstract class ApplianceJob extends Job {

	DockerAppliance appliance

	void init(DockerAppliance appliance) {
		this.appliance = appliance;
	}

	@Override
	def logPrefix() {
		"Appliance '${appliance.name}'"
	}

	@CompileStatic
	class Start extends ApplianceJob {

		void init(DockerAppliance appliance) {
			super.init(appliance)
			containerPreJobs(ContainerJob.Start)
		}

		@Override
		Answer doExecute() {
			logInfo "started"
			appliance.triggerStartedActions()
			Answer.success()
		}
	}

	@CompileStatic
	class Stop extends ApplianceJob {

		void init(DockerAppliance appliance) {
			super.init(appliance)
			containerPreJobs(ContainerJob.Stop)
		}

		@Override
		Answer doExecute() {
			logInfo "stopped"
			appliance.triggerStoppedActions()
			Answer.success()
		}
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	class Destroy extends ApplianceJob {

		void init(DockerAppliance appliance) {
			super.init(appliance);
			appliance.containers.each { DockerContainer c ->
				Job removeJob = preJob(ContainerJob.Remove, c)
				removeJob.preJob(ContainerJob.Kill, c)
			}
		}

		@Override
		public Answer doExecute() {
			logInfo 'destroyed'
			Answer.success()
		}
	}



	private containerPreJobs(Class<? extends ContainerJob> cls) {
		appliance.containers.each { DockerContainer c ->
			preJob(cls, c)
		}
	}
}
