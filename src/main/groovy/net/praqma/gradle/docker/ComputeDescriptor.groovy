package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import net.praqma.gradle.docker.jobs.ApplianceJob
import net.praqma.gradle.docker.jobs.ContainerJob
import net.praqma.gradle.docker.jobs.Job


@CompileStatic
@Immutable
class ComputeDescriptor {

	Class<Job> jobStartClass
	Class<Job> jobStopClass
	Class<Job> jobDestroyClass
	
	static final ComputeDescriptor appliance = new ComputeDescriptor(jobStartClass: ApplianceJob.Start, jobStopClass: ApplianceJob.Stop, jobDestroyClass: ApplianceJob.Destroy)
	static final ComputeDescriptor container = new ComputeDescriptor(jobStartClass: ContainerJob.Start, jobStopClass: ContainerJob.Stop, jobDestroyClass: ContainerJob.Remove)
}
