/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.blindpirate.gogradle.task

import com.github.blindpirate.gogradle.GogradleRunner
import com.github.blindpirate.gogradle.crossplatform.Arch
import com.github.blindpirate.gogradle.crossplatform.Os
import com.github.blindpirate.gogradle.task.go.GoBuildTask
import com.github.blindpirate.gogradle.task.go.GoExecutionAction
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor

import java.util.function.Consumer

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.isNull
import static org.mockito.Mockito.*

@RunWith(GogradleRunner)
class GoBuildTaskTest extends TaskTest {
    GoBuildTask task

    @Captor
    ArgumentCaptor cmdsCaptor
    @Captor
    ArgumentCaptor envCaptor

    @Before
    void setUp() {
        task = buildTask(GoBuildTask)
        task.setTargetPlatform('darwin-amd64,linux-386,windows-amd64')
        when(buildManager.getGopath()).thenReturn('project_gopath')
        when(setting.getPackagePath()).thenReturn('my/package')
    }

    @Test
    void 'adding default action should succeed'() {
        // when
        task.addDefaultActionIfNoCustomActions()
        task.actions.each { it.execute(task) }
        // then
        assert task.actions.size() == 1
        verify(buildManager, times(3)).go(cmdsCaptor.capture(), envCaptor.capture(), any(Consumer), any(Consumer), isNull())
        assert cmdsCaptor.allValues ==
                [['build', '-o', './.gogradle/${GOOS}_${GOARCH}_${PROJECT_NAME}', 'my/package'],
                 ['build', '-o', './.gogradle/${GOOS}_${GOARCH}_${PROJECT_NAME}', 'my/package'],
                 ['build', '-o', './.gogradle/${GOOS}_${GOARCH}_${PROJECT_NAME}', 'my/package'],
                ]
        assert envCaptor.allValues == [
                [GOOS: 'darwin', GOARCH: 'amd64', GOEXE: ''],
                [GOOS: 'linux', GOARCH: '386', GOEXE: ''],
                [GOOS: 'windows', GOARCH: 'amd64', GOEXE: '.exe']
        ]
    }

    @Test
    void 'default action should not be added if there is customized action'() {
        // when
        task.setTargetPlatform("${Os.hostOs}-${Arch.hostArch}")
        task.doLast {}
        task.addDefaultActionIfNoCustomActions()
        // then
        assert task.actions.size() == 1
    }

    @Test
    void 'custom action should be expanded when added in doLast'() {
        // when
        task.doLast {}
        // then
        assert task.actions.size() == 3
        assert task.actions.any { it instanceof GoExecutionAction }
    }

    @Test
    void 'custom action should be expanded when added in doFirst'() {
        // when
        task.doFirst {}
        // then
        assert task.actions.size() == 3
        assert task.actions.any { it instanceof GoExecutionAction }
    }

    @Test(expected = UnsupportedOperationException)
    void 'left shift should be unsupported'() {
        task << {}
    }

    @Test
    void 'execution of custom action should succeed'() {
        // when
        task.doLast {
            go 'build -o output'
        }
        task.actions.each { it.execute(task) }
        // then
        assert task.actions.size() == 3
        assert task.actions.any { it instanceof GoExecutionAction }
        assert task.singleBuildEnvironment == [:]
        verify(buildManager, times(3)).go(cmdsCaptor.capture(), envCaptor.capture(), any(Consumer), any(Consumer), isNull())
        assert cmdsCaptor.allValues == [
                ['build', '-o', 'output'],
                ['build', '-o', 'output'],
                ['build', '-o', 'output']
        ]
        assert envCaptor.allValues == [
                [GOOS: 'darwin', GOARCH: 'amd64', GOEXE: ''],
                [GOOS: 'linux', GOARCH: '386', GOEXE: '',],
                [GOOS: 'windows', GOARCH: 'amd64', GOEXE: '.exe']
        ]
    }

    @Test
    void 'other methods in GoExecutionAction should succeed'() {
        // when
        Closure c = {
            go 'build -o output'
        }

        task.doLast(c)
        // then
        task.actions.each { it.contextualise(null) }
        task.actions.each { assert it.classLoader == c.class.classLoader }
    }

    @Test
    void 'setting output location should succeed'() {
        task.outputLocation = 'outputlocation'
        assert task.outputLocation == 'outputlocation'
    }

    @Test(expected = NullPointerException)
    void 'exception should be thrown if closure throws an exception'() {
        task.doLast {
            throw new NullPointerException()
        }
        task.actions.each { it.execute(task) }
    }

    @Test(expected = IllegalStateException)
    void 'setting illegal target platform should result in an exception'() {
        task.targetPlatform = 'a-b,'
    }

    @Test(expected = IllegalArgumentException)
    void 'setting illegal os or arch should result in an exception'() {
        task.targetPlatform = 'a-b'
    }

    @Test(expected = IllegalArgumentException)
    void 'os must located at left'() {
        task.targetPlatform = 'amd64-linux'
    }

    @Test
    void 'setting target platform should succeed'() {
        task.targetPlatform = 'windows-amd64, linux-amd64, linux-386'
        assert task.targetPlatforms[0].left == Os.WINDOWS
        assert task.targetPlatforms[0].right == Arch.AMD64
        assert task.targetPlatforms[1].left == Os.LINUX
        assert task.targetPlatforms[1].right == Arch.AMD64
        assert task.targetPlatforms[2].left == Os.LINUX
        assert task.targetPlatforms[2].right == Arch.I386
    }

    @Test
    void 'pattern matching targetPlatform should be correct'() {
        assert isValidTargetPlatform('a-b')
        assert isValidTargetPlatform('\t a-b \n')
        assert !isValidTargetPlatform(' a -b ')
        assert !isValidTargetPlatform('\ta-b,\n')
        assert !isValidTargetPlatform('a-b,')
        assert !isValidTargetPlatform(' a-b, ')
        assert !isValidTargetPlatform(',a-b,')
        assert isValidTargetPlatform('a-b,1-a,c-d')
        assert isValidTargetPlatform('\t\t\na-b\n ,\n 1-a\t\n , c-2d  ')
    }

    boolean isValidTargetPlatform(String value) {
        return GoBuildTask.TARGET_PLATFORM_PATTERN.matcher(value).matches()
    }
}
