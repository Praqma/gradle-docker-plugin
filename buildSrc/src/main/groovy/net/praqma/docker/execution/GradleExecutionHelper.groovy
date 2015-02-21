package net.praqma.docker.execution

import org.apache.tools.ant.filters.ConcatFilter
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Sync
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection


class GradleExecutionHelper {

    private Project project

    private String taskNamePrefix

    private prepareGradleExecutionTask

    private File buildGradleTop

    private File classpathDir

    GradleExecutionHelper(Project project, Collection<File> classpath, String taskNamePrefix) {
        this.project = project
        this.taskNamePrefix = taskNamePrefix
        this.buildGradleTop = project.file("${project.buildDir}/tmp/gradleExecTop.txt")
        this.classpathDir = new File(project.buildDir, 'tmp/execClasspath')
        prepareGradleExecutionTask = project.tasks.create(name: 'prepareGradleExecution', type: Sync) {
            into classpathDir
            from classpath
            from project.jar
        }
    }

    Task createTask(String name, File dir, String taskName, String taskGroup = null) {
        File dest = new File(project.buildDir, "gradleExec/${name}")
        Task t = project.tasks.create(name: "syncGradleExecuteDir${name.capitalize()}", type: Sync, dependsOn: prepareGradleExecutionTask) {
            into dest

            from(dir) { exclude 'build.gradle' }
            from(dir) {
                include 'build.gradle'
                filter(ConcatFilter, prepend: buildGradleTop)
            }
            doFirst {
                assert dir.exists()
                buildGradleTop.text = "buildscript { dependencies { classpath fileTree(dir: '${classpathDir.path}') } }\n\n"
            }
        }
        project.tasks.create(name: "${taskNamePrefix}${name.capitalize()}", dependsOn: t) {
            if (taskGroup) group = taskGroup
            doLast { executeGradle(dest, taskName, '-s') }
        }
    }

    private void executeGradle(File dir, String taskName, String... args) {

        ProjectConnection connection = GradleConnector.newConnector().forProjectDirectory(dir).connect()
        BuildLauncher buildLauncher = connection.newBuild().
                setStandardOutput(System.out).setStandardError(System.err).
                //setColorOutput(true). valid from Gradle 2.3
                withArguments(args).forTasks(taskName)
        buildLauncher.run()
    }
}