package net.praqma.gradle.docker

import groovy.transform.CompileStatic


@CompileStatic
class DockerDslObject extends DockerObject implements NamedObjectsElement {

	final String name
	
	DockerDslObject(String name, DockerObject parent) {
		super(parent)
		this.name = name
	}
}
