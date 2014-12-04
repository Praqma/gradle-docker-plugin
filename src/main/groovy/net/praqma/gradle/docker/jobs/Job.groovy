package net.praqma.gradle.docker.jobs

import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.atomic.AtomicBoolean

@CompileStatic
abstract class Job extends ForkJoinTask<Object> {

	JobScheduler factory
	ExecutionContext execCtx
	final AtomicBoolean forked = new AtomicBoolean()
	synchronized final Collection<Job> preJobs = Collections.newSetFromMap(new ConcurrentHashMap())

	def rawResult

	Job doFork() {
		if (this.completedNormally) return this
		if (!this.forked.getAndSet(true)) {
			factory.submit(this)
		}
		this
	}

	protected boolean exec() {
		this.preJobs.each { Job j ->
			assert j != null
			j.join()
		}
		Answer answer = doExecute()
		assert answer != null
		switch (answer) {
			case Answer.Success:
				complete((answer as Answer.Success).value)
				return true
			case Answer.Failure:
				throw new RuntimeException((answer as Answer.Failure).reason)
			case Answer.Retry:
				factory.retryJob(this, answer as Answer.Retry)
				return isCompletedNormally()
			default:
				throw new RuntimeException('internal error')
		}
	}

	abstract Answer doExecute()

	Job preJob(Class<? extends Job> cls, Object...args) {
		Job job = factory.newJob(cls, args)
		this.preJobs << job
		job.doFork()
	}

	void preJobCompleted(Job preJobs) {
		boolean removed = this.preJobs.remove(preJobs)
		assert removed
	}

	boolean isReady() {
		preJobs.empty
	}

	void logInfo(msg) {
		print logPrefix()
		print ': '
		println msg.toString()
	}

	def logPrefix() {
		'general'
	}
}
