package net.praqma.gradle.docker

import groovy.transform.CompileStatic


@CompileStatic
abstract class DockerDslObject extends DockerObject {

	final String name

	DockerDslObject(String name, DockerObject parent) {
		super(parent)
		this.name = name
	}
}
