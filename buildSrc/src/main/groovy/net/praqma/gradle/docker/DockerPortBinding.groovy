package net.praqma.gradle.docker

import groovy.transform.CompileStatic;
import groovy.transform.Immutable;

@CompileStatic
@Immutable
class DockerPortBinding {
	int hostPort
	int containerPort

	String toString() {
		"${hostPort}:${containerPort}"
	}
}
