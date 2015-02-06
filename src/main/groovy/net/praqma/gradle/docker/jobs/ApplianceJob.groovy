package net.praqma.gradle.docker.jobs

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import net.praqma.gradle.docker.CompositeCompute
import net.praqma.gradle.docker.DockerAppliance
import net.praqma.gradle.docker.DockerContainer

import org.gradle.api.GradleException

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
			nestedPreJobs(ContainerJob.Start, ApplianceJob.Start, appliance)
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
			nestedPreJobs(ContainerJob.Stop, ApplianceJob.Stop, appliance)
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
			appliance.traverse(DockerContainer) { DockerContainer c ->
				Job removeJob = preJob(ContainerJob.Remove, c)
				removeJob.preJob(ContainerJob.Stop, c)
			}
		}

		@Override
		public Answer doExecute() {
			logInfo 'destroyed'
			Answer.success()
		}
	}



	private nestedPreJobs(Class<? extends ContainerJob> containerJobClass, Class<? extends ApplianceJob> applianceJobClass, CompositeCompute cc) {
		cc.eachCompute { c ->
			switch (c) {
				case DockerContainer:
					preJob(containerJobClass, c)
					break
				case DockerAppliance:
					preJob(applianceJobClass, c)
					break
				default:
					throw new GradleException("INTERNAL ERROR: ${c}")
			}
		}
	}
}
