package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic
import net.praqma.gradle.docker.jobs.BuildImageJob
import net.praqma.gradle.docker.json.JsonObjectExtractor
import net.praqma.gradle.utils.ProgressReporter

import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Copy

@CompileStatic
class LocalDockerImage extends DockerDslObject implements CopySpec {

	private File directory

	@Delegate(interfaces = false, includeTypes = CopySpec)
	final Copy copyTask

	String tag = 'latest'

	static String copyTaskName(String imageName) {
		taskName(imageName, 'Copy')
	}

	static String taskName(String imageName, String task) {
		"image${imageName.capitalize()}${task.capitalize()}"
	}

	String taskName(String task) {
		taskName(name, task)
	}

	@CompileDynamic
	LocalDockerImage(String name, DockerPluginExtension extension) {
		super(name, extension)
		directory = new File(project.buildDir, "dockerBuildImage/${name}/${tag}")

		copyTask = project.tasks.create(name: copyTaskName(name), type: Copy) { into directory } as Copy
	}

	void tag(String tag) {
		this.tag = tag
	}

	void dependsOn(obj) {
		copyTask.dependsOn(obj)
	}

	@CompileDynamic
	void createTasks() {
		createJobTask(taskName('Create'), BuildImageJob, this) {
			description "Create image '${this.name}'"
			dependsOn copyTask
		}
	}

	String build() {
		String nt = "${name}:${tag}"
		directory.mkdirs()
		assert directory != null && directory.exists()
		InputStream stream = dockerClient.buildImageCmd(directory).withTag(nt).exec() as InputStream
		JsonObjectExtractor jos = new JsonObjectExtractor(stream)
		def prefix = 'Successfully built '
		def id
		String status
		ProgressReporter.evaluate(project, "Building image ${name}") { ProgressReporter reporter ->
			jos.each {
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
		assert id != null
		id
	}

}
