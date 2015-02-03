package net.praqma.gradle.docker

import net.praqma.gradle.docker.test.ProjectTestCase

import org.gradle.api.tasks.Copy
import org.junit.Test

class DockerFileTest extends ProjectTestCase {

	@Test
	void testDockerFileInstructions() {
		def p = newRootProject()
		DockerFile dockerFile = new DockerFile(p, p.tasks.create(name: 'dummy',type: Copy))

		dockerFile
				.fromImage('ubuntu', 'latest')
				.maintainer('testcase')
				.run('shellcmd')
				.run('exec', 'format')
				.cmd('exec', 'format', 'for cmd')
				.expose(1234)
				.env('KEY', 'value')
				.add('/dest1', 'foo', 'boo')
				.copy('/dest2', 'bar')
				.volume('/vol1', '/vol2')
				.user('abc')
				.workdir('/dir')



		assert dockerFile.text ==
		"""FROM ubuntu:latest
MAINTAINER testcase
RUN shellcmd
RUN ["exec","format"]
CMD ["exec","format","for cmd"]
EXPOSE 1234
ENV KEY value
ADD foo boo /dest1
COPY bar /dest2
VOLUME ["/vol1","/vol2"]
USER abc
WORKDIR /dir
"""
	}
}
