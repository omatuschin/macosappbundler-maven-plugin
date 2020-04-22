/*
 * macOS app bundler Maven plugin
 * Copyright 2019 Christian Robert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.perdian.maven.plugins.macosappbundler.mojo;

import de.perdian.maven.plugins.macosappbundler.mojo.impl.AppGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.Map;

import static de.perdian.maven.plugins.macosappbundler.mojo.constant.PlistConstants.JVM_RUNTIME_PATH;
import static de.perdian.maven.plugins.macosappbundler.mojo.constant.PlistConstants.NATIVE_LIBRARY_PATH;

/**
 * Create all artifacts to publish a Java application as macOS application bundle.
 */

@Mojo(name = "bundle", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PACKAGE)
public class BundleMojo extends AbstractMojo {

    @Component
    private MavenProject project = null;

    @Parameter(required = true)
    private Map<String, String> plistVariables = null;

    @Parameter
    private String bundleJre = null;

    @Parameter
    private List<String> additionalResources = null;

    @Parameter
    private List<String> nativeLibraries = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.plistVariables.putIfAbsent("CFBundleDisplayName", this.project.getName());
        this.plistVariables.putIfAbsent("CFBundleName", this.project.getName());
        this.plistVariables.putIfAbsent("CFBundleIdentifier", this.project.getGroupId() + "." + this.project.getArtifactId());
        this.plistVariables.putIfAbsent("CFBundleShortVersionString", this.project.getVersion());
        this.plistVariables.putIfAbsent("CFBundleExecutable", "JavaLauncher");
        this.plistVariables.put(JVM_RUNTIME_PATH, "./Contents/PlugIns/Runtime.jre/Contents/Home");
        this.plistVariables.put(NATIVE_LIBRARY_PATH, "./Contents/Java/lib");

        String appName = StringUtils.defaultString(this.plistVariables.get("CFBundleName"), this.project.getBuild().getFinalName());
        File targetDirectory = new File(this.project.getBuild().getDirectory());
        File appDirectory = new File(targetDirectory, appName + ".app");
        this.getLog().info("Creating app directory at: " + appDirectory.getAbsolutePath());
        appDirectory.mkdirs();
        AppGenerator appGenerator = new AppGenerator(this.plistVariables, this.bundleJre, this.additionalResources, this.nativeLibraries, this.getLog());
        appGenerator.generateApp(this.project, appDirectory);
    }
}
