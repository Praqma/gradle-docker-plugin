package net.praqma.gradle.docker

import groovy.transform.CompileStatic

//@CompileStatic
class NamedObjects<T extends DockerDslObject, P extends DockerDslObject> {

	private final P parent
	private final Class<T> elementClass

	private final Map<String, T> objects = [:]

	private boolean frozen = false

	NamedObjects(P parent, Class<T> elementClass) {
		this.parent = parent
		this.elementClass = elementClass
	}

	T get(String name, Closure closure1 = null, Closure closure2 = null) {
		T object = objects[name] as T
		if (object == null) {
			if (frozen) throw new RuntimeException("${getClass().simpleName} is frozen. Trying to create object named: ${name}")
			object = elementClass.newInstance(name, parent) as T
			objects[name] = object as T
		}

		if (closure1) {
 		closure1.delegate = object
		closure1.resolveStrategy = Closure.DELEGATE_FIRST
		closure1(object)
		}

		if (closure2) {
			closure2.delegate = object
			closure2.resolveStrategy = Closure.DELEGATE_FIRST
			closure2(object)
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

	void each(@DelegatesTo(T) Closure closure) {
		new ArrayList(objects.values()).each(closure)
	}
}
