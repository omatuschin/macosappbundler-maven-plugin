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
package de.perdian.maven.plugins.macosappbundler.mojo.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.perdian.maven.plugins.macosappbundler.mojo.constant.PlistConstants.*;

public class AppGenerator {

    private final Map<String, String> plistVariables;
    private final String bundleJre;
    private final List<String> additionalResources;
    private final List<String> nativeLibraries;
    private final Log log;

    public AppGenerator(Map<String, String> plistVariables, String bundleJre, List<String> additionalResources, List<String> nativeLibraries, Log log) {
        this.plistVariables = plistVariables;
        this.bundleJre = bundleJre;
        this.additionalResources = additionalResources;
        this.nativeLibraries = nativeLibraries;
        this.log = log;
    }

    public void generateApp(MavenProject project, File appDirectory) throws MojoExecutionException {
        this.copyApplicationDependencies(project, new File(appDirectory, "Contents/Java"));
        this.copyNativeExecutable(new File(appDirectory, "Contents/MacOS"));
        if (this.bundleJre != null) {
            this.copyRuntime(new File(this.bundleJre), new File(appDirectory, "Contents/PlugIns/Runtime.jre/Contents/"));
        }
        if (this.additionalResources != null) {
            this.copyAdditionalResources(new File(appDirectory, "Contents/Resources/"));
        }
        if (this.nativeLibraries != null) {
            this.copyNativeLibraries(new File(appDirectory, "Contents/Java/lib/"));
        }
        this.generatePlist(project, new File(appDirectory, "Contents/"));
    }

    private void copyAdditionalResources(File resourcesDirectory) throws MojoExecutionException {
        for (String filename : this.additionalResources) {
            File resource = new File(filename);
            if (resource.exists()) {
                try {
                    FileUtils.copyFile(resource, new File(resourcesDirectory, resource.getName()));
                } catch (IOException exception) {
                    throw new MojoExecutionException("Cannot copy additional resource", exception);
                }
            } else {
                throw new MojoExecutionException("Specified additional resource does not exist");
            }
        }
    }

    private void copyNativeLibraries(File resourcesDirectory) throws MojoExecutionException {
        for (String filename : this.nativeLibraries) {
            File resource = new File(filename);
            if (resource.exists()) {
                try {
                    FileUtils.copyFile(resource, new File(resourcesDirectory, resource.getName()));
                } catch (IOException exception) {
                    throw new MojoExecutionException("Cannot copy native library", exception);
                }
            } else {
                throw new MojoExecutionException("Specified native library does not exist");
            }
        }
    }

    private void copyApplicationDependencies(MavenProject project, File appJavaDirectory)
            throws MojoExecutionException {
        this.getLog().info("Copy application dependencies to: " + appJavaDirectory.getAbsolutePath());
        try {
            if (StringUtils.isNotEmpty(this.plistVariables.get(JVM_MAIN_CLASS_NAME))) {
                this.copyClasspathApplicationDependencies(project, new File(appJavaDirectory, "classpath"));
            } else if (StringUtils.isNotEmpty(this.plistVariables.get(JVM_MAIN_MODULE_NAME))) {
                this.copyModuleApplicationDependencies(project, new File(appJavaDirectory, "modules"));
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot copy dependencies", e);
        }
    }

    private void copyClasspathApplicationDependencies(MavenProject project, File classpathDirectory)
            throws IOException {
        ArtifactRepositoryLayout repositoryLayout = new DefaultRepositoryLayout();
        this.copyClasspathApplicationDependencyArtifact(project.getArtifact(), classpathDirectory, repositoryLayout);
        for (Artifact artifact : project.getArtifacts()) {
            this.copyClasspathApplicationDependencyArtifact(artifact, classpathDirectory, repositoryLayout);
        }
    }

    private void copyClasspathApplicationDependencyArtifact(Artifact artifact, File targetDirectory,
                                                            ArtifactRepositoryLayout repositoryLayout) throws IOException {
        File targetFile = new File(targetDirectory, repositoryLayout.pathOf(artifact));
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        FileUtils.copyFile(artifact.getFile(), targetFile);
    }

    private void copyModuleApplicationDependencies(MavenProject project, File modulesDirectory)
            throws IOException {
        this.copyModuleApplicationDependencyArtifact(project.getArtifact(), modulesDirectory);
        for (Artifact artifact : project.getArtifacts()) {
            this.copyModuleApplicationDependencyArtifact(artifact, modulesDirectory);
        }
    }

    private void copyModuleApplicationDependencyArtifact(Artifact artifact, File modulesDirectory)
            throws IOException {
        StringBuilder targetFileName = new StringBuilder();
        targetFileName.append(artifact.getArtifactId());
        targetFileName.append("-").append(artifact.getVersion());
        targetFileName.append(".").append(FilenameUtils.getExtension(artifact.getFile().getName()));
        File targetFile = new File(modulesDirectory, targetFileName.toString());
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        FileUtils.copyFile(artifact.getFile(), targetFile);
    }

    private void copyNativeExecutable(File targetDirectory) throws MojoExecutionException {
        try {
            URL nativeExecutableSource = this.getClass().getClassLoader().getResource("JavaLauncher");
            if (nativeExecutableSource == null) {
                throw new MojoExecutionException("No native executable packaged in plugin");
            } else {
                String targetFileName = StringUtils.defaultIfEmpty(this.plistVariables.get(CF_BUNDLE_EXECUTABLE), "JavaLauncher");
                File targetFile = new File(targetDirectory, targetFileName);
                this.getLog().info("Copy native executable to: " + targetFile.getAbsolutePath());
                try (InputStream nativeExecutableStream = nativeExecutableSource.openStream()) {
                    FileUtils.copyToFile(nativeExecutableStream, targetFile);
                }
                targetFile.setExecutable(true);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot copy native executable", e);
        }
    }

    private void copyRuntime(File sourceDirectory, File targetDirectory)
            throws MojoExecutionException {
        if (sourceDirectory.exists()) {

            try {
                FileUtils.copyDirectory(sourceDirectory, targetDirectory);
            } catch (IOException exception) {
                throw new MojoExecutionException(
                        MessageFormat.format("Failed to bundle JRE because the JRE could not be copied: {0}",
                                exception.toString()), exception);
            }

        } else {
            throw new MojoExecutionException(
                    "Failed to bundle JRE because the JRE could not be found!");
        }
    }

    private void generatePlist(MavenProject project, File contentsDirectory)
            throws MojoExecutionException {

        try {
            File plistFile = new File(contentsDirectory, "Info.plist");
            this.getLog().info("Generating Info.plist");

            Map<String, String> plistVariables = new HashMap<>(this.plistVariables);
            plistVariables.put(CF_BUNDLE_ICON_FILE, this.copyIcon(contentsDirectory));

            FileUtils.write(plistFile, this.toXmlString(project, plistVariables), "UTF-8");
        } catch (Exception e) {
            throw new MojoExecutionException("Cannot generate Info.plist file", e);
        }
    }

    public String toXmlString(MavenProject project, Map<String, String> plistVariables) throws Exception {

        StringBuilder plistBuilder = new StringBuilder();

        List<String> plistLines = Files.readAllLines(project.getBasedir().toPath().resolve(Paths.get("packaging")).resolve("Info.plist"));
        for (String plistLine : plistLines) {
            while (plistLine.contains("${") && plistLine.contains("}")
                    && plistLine.indexOf("}") > plistLine.indexOf("${")) {
                plistBuilder.append(plistLine, 0, plistLine.indexOf("${"));
                String variableName = plistLine
                        .substring(plistLine.indexOf("${") + 2, plistLine.indexOf("}"));

                if (plistVariables.get(variableName) != null) {
                    plistBuilder.append(plistVariables.get(variableName));
                }
                plistLine = plistLine.substring(plistLine.indexOf("}") + 1);
            }
            plistBuilder.append(plistLine);
        }

        return plistBuilder.toString();

    }

    private String copyIcon(File contentsDirectory)
            throws MojoExecutionException {
        String iconFileValue = this.plistVariables.get(CF_BUNDLE_ICON_FILE);
        if (StringUtils.isNotEmpty(iconFileValue)) {
            File iconFile = new File(iconFileValue);
            if (!iconFile.exists()) {
                throw new MojoExecutionException(
                        "Cannot find declared icon file " + iconFile.getName() + " at: " + iconFile
                                .getAbsolutePath());
            } else {
                File resourcesDirectory = new File(contentsDirectory, "Resources");
                File targetFile = new File(resourcesDirectory, iconFile.getName());
                if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs();
                }
                try {
                    FileUtils.copyFile(iconFile, targetFile);
                    return targetFile.getName();
                } catch (IOException e) {
                    throw new MojoExecutionException(
                            "Cannot copy icon file to: " + targetFile.getAbsolutePath(), e);
                }
            }
        } else {
            return null;
        }
    }

    private Log getLog() {
        return this.log;
    }
}
