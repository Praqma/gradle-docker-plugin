package net.praqma.gradle.docker.jobs;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.junit.Test

class JobSchedulerTest {

	@Test
	void testFactory() {
		JobScheduler factory = new JobScheduler()
		Job j1 = factory.newJob(TestFactoryJob, 1, "a", "b")
		Job j2 = factory.newJob(TestFactoryJob, 1, "a", "b")
		assert j1.is(j2)
	}

	@Test
	void testExecutionOrder() {
		def scheduler = new JobScheduler()
		Job job1 = scheduler.newJob(TestJob, 1)
		scheduler.launch(job1)
		assert TestJob.list*.n == [4, 3, 2, 1]
	}

	@Test
	void testRetry() {
		def scheduler = new JobScheduler()
		Job job = scheduler.newJob(TestRetryJob)
		scheduler.launch(job)
		assert job.get() == 0
	}
	
	@Test
	void testCyclicJobDependency() {
		JobScheduler scheduler = new JobScheduler()
		Job j1 = scheduler.newJob(TestFactoryJob, 1)
		Job j2 = scheduler.newJob(TestFactoryJob, 2)
		
	}
}

class TestFactoryJob extends Job {

	int n
	String a
	String b

	void init(int n, String a = null, String b = null) {
		this.n = n
		this.a = a
		this.b = b
	}

	@Override
	Answer doExecute() {
	}

	@Override
	public String toString() {
		getClass().simpleName + ":" + n
	}
}

@CompileStatic
class TestJob extends Job {

	static List<Job> list = Collections.synchronizedList([])

	int n

	void init(int n) {
		this.n = n
		switch (n) {
			case 1:
				preJob(TestJob, 2)
				preJob(TestJob, 2)
				break
			case 2:
				preJob(TestJob, 3)
				preJob(TestJob, 4)
				break
			case 3:
				preJob(TestJob, 4)
				break
		}
	}

	@Override
	Answer doExecute() {
		list << this
		Answer.success(n)
	}

	@Override
	public String toString() {
		getClass().simpleName + ":" + n
	}
}

@CompileStatic
class TestRetryJob extends Job {
	int count = 3

	void init() {}

	@Override
	public Answer doExecute() {
		if (--count == 0) {
			return Answer.success(0)
		} else {
			return Answer.retry(1)
		}
	}
}