package net.praqma.gradle.docker.json

import org.apache.commons.io.IOUtils;

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
class JsonObjectExtractor implements Iterator<Object> {

	private final SplittingInputStream stream;
	private JsonSlurper slurper = new JsonSlurper()

	JsonObjectExtractor(InputStream inputStream) {
		this.stream = new SplittingInputStream(inputStream)
	}

	def next() {
		if (this.stream.nextStream()) {
			if (false) { // TODO control via flag or some other mechanism
				byte[] bytes = IOUtils.toByteArray(stream)
				println "STREAM " + new String(bytes)
				return slurper.parse(new ByteArrayInputStream(bytes))
			} else {
				return slurper.parse(this.stream)
			}
		}
		throw new NoSuchElementException()
	}

	@Override
	public boolean hasNext() {
		this.stream.nextStream()
	}

	void remove() {
		throw new UnsupportedOperationException()
	}
}

/**
 * Helper class used to parse json objects from a stream where one json object follows the next.
 * <p>
 * When a closing curly bracket is followed by an opening curly bracket it is assumed that a json object ends, and a
 * new starts. This is good enough for what is needed, but not a general solution, e.g. doesn't handle arrays, and the pair
 * could be in a string literal.
 * <p>
 * The class will split an underlying stream in sub-stream, one per json object. The method nextStream() is used
 * to advance to the next json object.
 */
@CompileStatic
class SplittingInputStream extends InputStream {

	/** The underlying InputStream */
	private InputStream stream

	/** Flag indicating if the last seen non-whitespace character is a close curly bracket */
	private boolean closingBracket = false

	/** If greater or equal to zero this is returned by the next call to read(). The field is used to implement peek functionality */
	private int next

	/** Set to true when EOF is reached for the current sub-stream */
	private boolean eof = true

	public SplittingInputStream(InputStream stream) {
		this.stream = stream
		this.next = stream.read()
	}

	@Override
	public int read() throws IOException {
		if (eof) return -1
		int answer = this.next
		if (answer >= 0) {
			next = -1
		} else {
			answer = this.stream.read()
			if (answer == '}') {
				this.closingBracket = true
			} else if (this.closingBracket) {
				if (answer == '{') {
					this.next = answer
					eof = true
					answer = -1
				} else if (!Character.isWhitespace(answer)){
					this.closingBracket = false
				}
			}
		}
		assert answer != 0
		return answer
	}

	/**
	 * Advance to the next sub-stream. If called multiple times without read is called, not advancement is done.
	 *
	 * @return true if a new sub-stream is found, otherwise false
	 */
	public boolean nextStream() {
		if (next != -1) {
			this.eof = false
			this.closingBracket = false
			return true
		}
		false
	}
}
