package net.praqma.gradle.docker;

import groovy.transform.CompileStatic
import net.praqma.gradle.docker.test.ProjectTestCase

import org.junit.Test

@CompileStatic
public class DockerPluginExtensionTest extends ProjectTestCase {

	@Test
	void testCreation() {
		def p = newRootProject(false)
		new DockerPluginExtension(p)
	}
}
