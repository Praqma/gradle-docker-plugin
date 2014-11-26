package net.praqma.gradle.docker.json;

import static org.junit.Assert.*
import net.praqma.gradle.docker.json.SplittingInputStream;

import org.junit.Test

import com.google.common.io.ByteStreams

class SplittingInputStreamTest {

	@Test
	public void testEmpty() {
		SplittingInputStream stream = new SplittingInputStream(new ByteArrayInputStream(new byte[0]))
		assert !stream.nextStream()
	}

	@Test
	public void test() {
		InputStream stream = new ByteArrayInputStream("{1}{2 }\n{{} 3} {4\n}".bytes)
		SplittingInputStream sis = new SplittingInputStream(stream)
		assert sis.read() == -1
		assert sis.nextStream()
		assertStream(sis, "{1}")
		assert sis.nextStream()
		assert sis.nextStream()
		assertStream(sis, "{2 }\n")
		assert sis.nextStream()
		assertStream(sis, "{{} 3} ")
		assert sis.nextStream()
		assert sis.nextStream()
		assertStream(sis, "{4\n}")
		assert !sis.nextStream()
		assert !sis.nextStream()
	}

	private assertStream(InputStream stream, String expected) {
		String s = new String(ByteStreams.toByteArray(stream))
		assert s == expected
		assert stream.read() == -1
	}

}
