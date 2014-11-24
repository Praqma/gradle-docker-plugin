package net.praqma.gradle.utils

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.gradle.api.Project
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory

@CompileStatic
class ProgressReporter {

	private ProgressLogger progressLogger

	static void evaluate(Project project, String description, Closure closure) {
		ProgressReporter progressReporter = new ProgressReporter()
		ProgressLogger logger = createProgressLoggerFactory(project).newOperation(Project)
		logger.description = description
		logger.started()
		progressReporter.progressLogger = logger
		closure(progressReporter)
		progressReporter.progressLogger.completed()
	}

	void update(msg) {
		def s = format(msg)
		progressLogger.progress(s)
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	private static String format(message) {
		if (message instanceof Closure) {
			message = message()
		}
		((message as String)?:"").replace("\n", " ")
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	private static ProgressLoggerFactory createProgressLoggerFactory(Project project) {
		// How do we get a ProgressLoggerFactory instance. Seems we need to use internal services api on project
		project.services.get(ProgressLoggerFactory)
	}
}


