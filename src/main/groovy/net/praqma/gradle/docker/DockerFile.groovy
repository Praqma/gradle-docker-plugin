package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import net.praqma.gradle.docker.tasks.CreateDockerFileTask

import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.AbstractCopyTask

@CompileStatic
public class DockerFile {

	String fromImage


	private final Project project

	private final AbstractCopyTask copyTask

	private final CreateDockerFileTask createDockerFileTask

	@CompileStatic(TypeCheckingMode.SKIP)
	DockerFile(Project project, AbstractCopyTask copyTask) {
		this.project = project
		this.copyTask = copyTask
		File dockerFile = new File(project.buildDir, "tmp/${copyTask.name}/Dockerfile")
		this.createDockerFileTask = project.tasks.create(name: "dockerfileFor${copyTask.name.capitalize()}", type: CreateDockerFileTask) { file = dockerFile }
		copyTask.from (dockerFile)
		copyTask.dependsOn this.createDockerFileTask
	}

	String getText() {
		return createDockerFileTask.text
	}
	
	DockerFile fromImage(String image, String tag = null) {
		String line = tag == null ? image : "${image}:${tag}"
		fromImage = line
		createDockerFileTask.appendLine 'FROM', line
		this
	}

	DockerFile maintainer(String maintainer) {
		createDockerFileTask.appendLine 'MAINTAINER', maintainer
		this
	}

	DockerFile run(String command) {
		createDockerFileTask.appendLine 'RUN', command
		this
	}

	DockerFile run(String ...command) {
		createDockerFileTask.appendLine 'RUN', command
		this
	}

	DockerFile cmd(String cmd) {
		createDockerFileTask.appendLine 'CMD', cmd
		this
	}

	DockerFile cmd(String ...cmd) {
		createDockerFileTask.appendLine 'CMD', cmd
		this
	}

	DockerFile expose(int port) {
		createDockerFileTask.appendLine 'EXPOSE', port as String
		this
	}

	DockerFile env(String key, String value) {
		createDockerFileTask.appendLine 'ENV', "${key} ${value}"
		this
	}
	// TODO add env method with map arg

	/**
	 * Note that dest is first argument. In DockerFile it is the last arguments.
	 *  
	 */
	DockerFile add(String dest, Object ...src) {
		createDockerFileTask.appendLine 'ADD', "${fileSources(true, src)} ${dest}"
		this
	}

	/**
	 * Note that dest is first argument. In DockerFile it is the last arguments.
	 *  
	 */
	DockerFile copy(String dest, Object ...src) {
		createDockerFileTask.appendLine 'COPY', "${fileSources(false, src)} ${dest}"
		this
	}

	DockerFile volume(String ...volumes) {
		createDockerFileTask.appendLine 'VOLUME', volumes
		this
	}

	DockerFile user(String user) {
		createDockerFileTask.appendLine 'USER', user
		this
	}

	DockerFile workdir(String dir) {
		createDockerFileTask.appendLine 'WORKDIR', dir
		this
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	private String fileSources(boolean allowUrls, Object...sources) {
		String s = sources.collect { src ->
			String srcString
			switch (src) {
				case File:
					File file = src as File
					String s = file.absolutePath
					srcString = copyToCtx(src, file.name)
					break
				case Task:
					Task task = src as Task
					srcString = copyToCtx(src, task.name)
					break
				case String:
				case GString:
					srcString = src
					break
				case Iterable:
                    // TODO implement
				default:
					throw new GradleException("Can't use ${src} as source for file (class: ${src.class})")
					break
			}
			assert srcString != null
			srcString
		}.join(' ')
		s
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	private String copyToCtx(src, String name) {
		assert name != null
		copyTask.into('__tmp__') {
			from(src)
			rename { name }
		}
		"__tmp__/${name}"
	}

}
