package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import net.praqma.gradle.docker.json.JsonObjectExtractor
import net.praqma.gradle.docker.tasks.BuildImageTask
import net.praqma.gradle.utils.ProgressReporter

@CompileStatic
class LocalDockerImage extends DockerDslObject {

	private File directory
	String tag

	LocalDockerImage(String name, DockerPluginExtension extension) {
		super(name, extension);
	}

	void directory(File dir) {
		this.directory = dir
	}

	void tag(String tag) {
		this.tag = tag
	}

	String build() {
		assert directory != null && directory.exists()
		InputStream stream = dockerClient.buildImageCmd(directory).withTag("${name}:${tag}").exec() as InputStream
		JsonObjectExtractor jos = new JsonObjectExtractor(stream)
		def prefix = 'Successfully built '
		def id
		String status
		def jhs
		ProgressReporter.evaluate(project, "Building image ${name}") {
			ProgressReporter reporter ->
			jos.each {
				jhs = it
				String s
				if (it['stream']) {
					s = it['stream'].toString().trim()
					if (s.startsWith(prefix)) {
						id = s[prefix.size()..-1]
					}
				} else {
					status = it['status']
					String progress = it['progress']
					if (progress) {
						s = "${status}: ${progress}"
					} else {
						s = status
					}
				}
				reporter.update("Building ${name}: ${s}".replace("\n", " "))
			}
		}
		assert id != null : "JHS ${jhs}"
		id
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	void createTasks() {
		project.tasks.create(name: "dockerImageBuild" + name.capitalize(), type: BuildImageTask, group: 'Docker') {
			dockerImage = this
		}
	}
}
