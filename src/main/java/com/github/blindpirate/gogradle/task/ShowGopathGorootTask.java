package com.github.blindpirate.gogradle.task;

import com.github.blindpirate.gogradle.crossplatform.GoBinaryManager;
import com.google.inject.Inject;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Arrays;

import static com.github.blindpirate.gogradle.task.GolangTaskContainer.RESOLVE_BUILD_DEPENDENCIES_TASK_NAME;
import static com.github.blindpirate.gogradle.task.GolangTaskContainer.RESOLVE_TEST_DEPENDENCIES_TASK_NAME;
import static com.github.blindpirate.gogradle.util.StringUtils.toUnixString;

public class ShowGopathGorootTask extends AbstractGolangTask {
    private static final Logger LOGGER = Logging.getLogger(ShowGopathGorootTask.class);

    @Inject
    private GoBinaryManager goBinaryManager;

    public ShowGopathGorootTask() {
        dependsOn(RESOLVE_BUILD_DEPENDENCIES_TASK_NAME, RESOLVE_TEST_DEPENDENCIES_TASK_NAME);
    }

    @TaskAction
    public void showGopathGoroot() {
        File projectRoot = getProject().getRootDir();
        String projectGopath = toUnixString(projectRoot.toPath().resolve(".gogradle/project_gopath").toAbsolutePath());
        String buildGopath = toUnixString(projectRoot.toPath().resolve(".gogradle/build_gopath").toAbsolutePath());
        String testGopath = toUnixString(projectRoot.toPath().resolve(".gogradle/test_gopath").toAbsolutePath());

        String gopath = String.join(File.pathSeparator, Arrays.asList(projectGopath, buildGopath, testGopath));

        LOGGER.quiet("GOPATH: {}", gopath);
        LOGGER.quiet("GOROOT: {}", toUnixString(goBinaryManager.getGoroot()));
    }
}
