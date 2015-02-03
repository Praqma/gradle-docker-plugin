package net.praqma.gradle.docker

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.praqma.gradle.docker.jobs.BuildImageJob
import net.praqma.gradle.docker.json.JsonObjectExtractor
import net.praqma.gradle.utils.ProgressReporter

import org.gradle.api.GradleException
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Sync

@CompileStatic
class LocalDockerImage extends DockerDslObject implements CopySpec {

	LocalDockerImage baseImage
	
	private File directory

	private DockerFile dockerFile

	@Delegate(interfaces = false, includeTypes = CopySpec)
	final Sync copyTask

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
		directory = new File(project.buildDir, "dockerBuildImage/${name}/${tag}/ctx")
		directory.mkdirs()

		copyTask = project.tasks.create(name: copyTaskName(name), type: Sync) {  into directory } as Sync
	}

	void dockerFile(Closure closure) {
		if (this.@dockerFile == null) {
			this.@dockerFile = new DockerFile(project, copyTask)
		}
		closure.delegate = this.@dockerFile
		closure(this.@dockerFile)
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
				if (it['error']) {
					throw new GradleException("Build image ${nt}: ${it['error']}")
				}
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
	
	@CompileDynamic
	void postProcess() {
		String baseImageName = dockerFile?.fromImage
		if (baseImageName) {
			baseImage = dockerExtension.getImage(baseImageName)
			if (baseImage) {
				copyTask.dependsOn baseImage.copyTask
			}
		}
	}
}
