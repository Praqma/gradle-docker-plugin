package net.praqma.gradle.docker.tasks

import net.praqma.gradle.docker.jobs.Job
import net.praqma.gradle.docker.jobs.JobScheduler

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class JobBasedTask extends DefaultTask {

	@Input
	Class<? extends Job> jobClass

	@Input
	Object[] args

	@TaskAction
	void executeJob() {
		JobScheduler.execute(jobClass, *args)
	}
}
