package net.praqma.gradle.docker.jobs

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

@CompileStatic
class JobScheduler {

	private final ForkJoinPool pool = new ForkJoinPool(25)
	private final Map<List, Job> cache = [:]
	private Collection<Job> pendingSubmits = []

	static void execute(Class<? extends Job> jobClass, Object...args) {
		def scheduler = new JobScheduler()
		Job job = scheduler.newJob(jobClass, args)
		scheduler.launch(job)
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	Job newJob(Class<? extends Job> cls, Object...args) {
		assert cls != null
		List key = [cls]
		key.addAll(args)
		Job job = cache[key]
		if (job == null) {
			job = cls.newInstance()
			job.factory = this
			cache[key] = job
			job.init(*args)
		}
		assert job != null
		job
	}

	void submit(Job job) {
		if (pendingSubmits == null) {
			pool.submit(job)
		} else {
			pendingSubmits << job
		}
	}

	void submitDelayed(Job job, int delayMs) {
		sleep delayMs
		submit(job)
	}

	void retryJob(Job j, Answer.Retry retryParams) {
		RetryJob retry = new RetryJob(j)
		retry.factory = this
		submitDelayed(retry, retryParams.delayMs)
		resultFromJob(j, retry)
	}

	void resultFromJob(Job tgtJob, ForkJoinTask<Object> srcJob){
		try {
			tgtJob.complete(srcJob.join())
		} catch (Exception e) {
			tgtJob.completeExceptionally(e)
		}
	}

	void launch(Job theJob) {
		assert theJob != null
		pendingSubmits.each { pool.submit(it) }
		pendingSubmits = null // new submits are submitted immediately
		pool.invoke(theJob)
	}
}

class RetryJob extends Job {

	final Job job

	RetryJob(Job job) {
		this.job = job instanceof RetryJob ? job.job : job
	}

	@Override
	public Answer doExecute() {
		logInfo "Retrying ${job}"
		job.doExecute()
	}
}
