package net.praqma.gradle.docker.jobs

import groovy.transform.CompileStatic

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
class ExecutionContext {

	final int jobCount
	final AtomicInteger completedJobs = new AtomicInteger()

	ExecutionContext(int jobCount) {
		this.jobCount = jobCount
	}

	boolean isDone() {
		this.jobCount == this.completedJobs.get()
	}
}
