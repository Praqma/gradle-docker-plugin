package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection

@CompileStatic
public class DockerFile {

	private File file

	private CopySpec copySpec

	@CompileStatic(TypeCheckingMode.SKIP)
	DockerFile(File file, CopySpec copySpec) {
		this.file = file
		this.copySpec = copySpec
		copySpec.from file
		file.text = ''
	}

	DockerFile fromImage(String image, String tag = null) {
		String line = tag == null ? image : "${image}:${tag}"
		appendLine 'FROM', line
		this
	}

	DockerFile maintainer(String maintainer) {
		appendLine 'MAINTAINER', maintainer
		this
	}

	DockerFile run(String command) {
		appendLine 'RUN', command
		this
	}

	DockerFile run(String ...command) {
		appendLine 'RUN', command
		this
	}

	DockerFile cmd(String cmd) {
		appendLine 'CMD', cmd
		this
	}

	DockerFile cmd(String ...cmd) {
		appendLine 'CMD', cmd
		this
	}

	DockerFile expose(int port) {
		appendLine 'EXPOSE', port as String
		this
	}

	DockerFile env(String key, String value) {
		appendLine 'ENV', "${key} ${value}"
		this
	}
	// TODO add env method with map arg

	/**
	 * Note that dest is first argument. In DockerFile it is the last arguments.
	 *  
	 */
	DockerFile add(String dest, Object ...src) {
		appendLine 'ADD', "${fileSources(true, src)} ${dest}"
		this
	}


	/**
	 * Note that dest is first argument. In DockerFile it is the last arguments.
	 *  
	 */
	DockerFile copy(String dest, Object ...src) {
		appendLine 'COPY', "${fileSources(false, src)} ${dest}"
		this
	}

	DockerFile volume(String ...volumes) {
		appendLine 'VOLUME', volumes
		this
	}

	DockerFile user(String user) {
		appendLine 'USER', user
		this
	}

	DockerFile workdir(String dir) {
		appendLine 'WORKDIR', dir
		this
	}

	private void appendLine(String instruction, String line) {
		file << instruction << " " << line << "\n"
	}

	private void appendLine(String instruction, String ...line) {
		file << instruction << " " << toJsonArray(line) << "\n"
	}

	private String toJsonArray(String[] ary) {
		new groovy.json.JsonBuilder(ary).toString()
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	private String fileSources(boolean allowUrls, Object...sources) {
		String s = sources.collect { src ->
			String srcString
			switch (src) {
				case File:
					File file = src as File
					String s = file.absolutePath
					copyToCtx(src, DigestUtils.sha1Hex(s))
					break
				case Task:
					Task task = src as Task
					copyToCtx(src, task.name)
					break
				case String:
				case GString:
					srcString = src
					break
				case FileCollection:
				default:
					throw new GradleException("Can't use ${src} as source for file")
					break
			}
			srcString
		}.join(' ')
		s
	}
	
	@CompileStatic(TypeCheckingMode.SKIP)
	private String copyToCtx(Osrc, String name) {
		copySpec.into('__tmp__') {
			from(src)
			rename { name }
		}
		"__tmp__/${name}"
	}
}
