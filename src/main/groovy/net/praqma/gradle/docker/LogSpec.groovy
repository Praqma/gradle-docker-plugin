package net.praqma.gradle.docker;

import groovy.transform.ToString

import com.github.dockerjava.api.command.LogContainerCmd

@ToString
class LogSpec {

	int tail = -1
	boolean stderr = true, stdout = true, timestamps = false, followStream = false

	void applyToCmd(LogContainerCmd cmd) {
		cmd
				.withFollowStream(followStream)
				.withStdOut(stdout)
				.withStdErr(stderr)
				.withTimestamps(timestamps)
		if (tail == -1) {
			cmd.withTailAll()
		} else {
			cmd.withTail(tail)
		}
	}
}
