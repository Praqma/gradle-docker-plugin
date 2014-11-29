package net.praqma.gradle.docker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*

import org.junit.Test

class NamedObjectsTest {

	@Test
	void testLookup() {
		NamedObjects<NamedObjectsElement, DockerDslObject> no = new NamedObjects<>(new DockerDslObject("test", null))
		Element e1 = no.getObject('x', Element)
		Element e2 = no.getObject('x', Element)
		Element e3 = no.getObject('x', Element) { }
		
		assertThat e1, is(sameInstance(e2)) 
		assertThat e1, is(sameInstance(e3))
		
	}
}

class Element implements NamedObjectsElement {
	final String name
	Element(String name, Object parent) {
		this.name = name
	}
}