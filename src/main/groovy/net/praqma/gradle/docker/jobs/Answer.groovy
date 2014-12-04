package net.praqma.gradle.docker.jobs

import groovy.transform.CompileStatic

class Answer {

	static class Success extends Answer {
		final def value

		private Success(value) {
			this.value=value
		}
	}

	static class Failure extends Answer {
		String reason
	}


	static class Retry extends Answer {
		final int delayMs

		private Retry(int delayMs) {
			this.delayMs = delayMs
		}
	}

	static Answer success(value = null) {
		new Success(value)
	}

	static Answer retry(int delayMs) {
		new Retry(delayMs)
	}
}
