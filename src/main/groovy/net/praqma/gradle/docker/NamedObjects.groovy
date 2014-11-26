package net.praqma.gradle.docker

import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic

@CompileStatic
class NamedObjects<T extends DockerDslObject, P extends DockerDslObject> {

	private final P parent
	private final Class<T> elementClass

	private final Map<String, T> objects = [:]

	private boolean frozen = false

	NamedObjects(P parent, Class<T> elementClass) {
		this.parent = parent
		this.elementClass = elementClass
	}

	T getObject(String name, Closure closure = null) {
		T object = objects[name] as T
		if (object == null) {
			if (frozen) throw new RuntimeException("${getClass().simpleName} is frozen. Trying to create object named: ${name}")
			object = elementClass.newInstance(name, parent) as T
			objects[name] = object as T
		}

		if (closure) {
			closure.delegate = object
			closure.resolveStrategy = Closure.DELEGATE_FIRST
			closure(object)
		}

		object
	}

	boolean hasObject(String name) {
		return null != this.objects[name]
	}

	void postProcess() {
		objects.values().each { T t -> t.postProcess() }
		this.frozen = true
	}

	@CompileDynamic
	void each(@DelegatesTo(T) Closure closure) {
		new ArrayList(objects.values()).each(closure)
	}
}
