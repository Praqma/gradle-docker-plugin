package net.praqma.gradle.docker

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.gradle.api.GradleException

@CompileStatic
class NamedObjects<T extends NamedObjectsElement> {

	private final DockerDslObject parent

	private final Map<String, T> objects = [:]

	private boolean frozen = false

	NamedObjects(DockerDslObject parent) {
		assert parent != null
		this.parent = parent
	}

	T getObject(String name, Class<?> expectedClass, Closure closure = null) {
		T object = objects[name] as T
		if (object == null) {
			if (frozen) throw new GradleException("${getClass().simpleName} is frozen. Trying to create object named: ${name}")
			object = expectedClass.newInstance(name, parent) as T
			objects[name] = object as T
		} 

		if (closure) {
			closure.delegate = object
			closure.resolveStrategy = Closure.DELEGATE_FIRST
			closure(object)
		}

		object
	}

	T get(String name) {
		this.objects[name] as T
	}
	
	boolean hasObject(String name) {
		return null != this.objects[name]
	}

	@CompileDynamic
	void each(@DelegatesTo(T) Closure closure) {
		new ArrayList(objects.values()).each(closure)
	}
}

interface NamedObjectsElement {
	String getName()
}

