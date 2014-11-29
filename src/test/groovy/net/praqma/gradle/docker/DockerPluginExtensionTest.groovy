package net.praqma.gradle.docker;

import net.praqma.gradle.docker.test.ProjectTestCase

import org.gradle.api.Project
import org.junit.Test

public class DockerPluginExtensionTest extends ProjectTestCase {

	@Test
	void testCreation() {
		def p = newRootProject(false)
		new DockerPluginExtension(p)
	}

	@Test
	void testPostProcess() {
		Project p = newRootProject()
		p.with {
			docker {
				appliance (newName('app')) {
					container (newName('con')) {
					}
				}
			}
		}
		p.docker.postProcess()
	}
}
