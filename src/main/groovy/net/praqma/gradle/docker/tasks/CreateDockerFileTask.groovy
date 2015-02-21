package net.praqma.gradle.docker.tasks

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
class CreateDockerFileTask extends DefaultTask {

    private StringBuffer buffer = new StringBuffer()

    @OutputFile
    File file

    @TaskAction
    void createFile() {
        file.text = text
    }

    @Input
    String getText() {
        return buffer.toString()
    }

    void appendLine(String instruction, String line) {
        buffer << instruction << " " << line << "\n"
    }

    void appendLine(String instruction, String... line) {
        buffer << instruction << " " << toJsonArray(line) << "\n"
    }

    private String toJsonArray(String[] ary) {
        new JsonBuilder(ary).toString()
    }


}
