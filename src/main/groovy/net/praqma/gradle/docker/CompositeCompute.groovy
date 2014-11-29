package net.praqma.gradle.docker;


trait CompositeCompute {

	private final NamedObjects<DockerCompute> computes


	void initCompositeCompute(DockerDslObject parent) {
		this.computes = new NamedObjects<>(parent)
	}

	DockerAppliance appliance(String name, Closure configBlock) {
		computes.getObject(name, DockerAppliance, configBlock) as DockerAppliance
	}

	DockerAppliance appliance(String name) {
		computes.getObject(name, DockerAppliance) as DockerAppliance
	}

	DockerContainer container(String name, Closure configBlock) {
		computes.getObject(name, DockerContainer, configBlock) as DockerContainer
	}

	DockerContainer container(String name) {
		computes.getObject(name, DockerContainer) as DockerContainer
	}

	void eachCompute(Closure closure) {
		computes.each closure
	}

	void eachContainerDeep(@DelegatesTo(DockerContainer) Closure closure) {
		eachCompute { m ->
			if (m instanceof DockerContainer) {
				closure(m)
			}
			if (m instanceof CompositeCompute) {
				m.eachContainerDeep closure
			}
		}
	}

	abstract String getName()
}
