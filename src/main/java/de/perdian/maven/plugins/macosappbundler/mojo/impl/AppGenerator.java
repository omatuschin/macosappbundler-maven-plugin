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

import de.perdian.maven.plugins.macosappbundler.mojo.model.PlistConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class AppGenerator {

  private final PlistConfiguration plistConfiguration;
  private final String bundleJre;
  private final Log log;

  public AppGenerator(PlistConfiguration plistConfiguration, String bundleJre, Log log) {
    this.plistConfiguration = plistConfiguration;
    this.bundleJre = bundleJre;
    this.log = log;
  }

  public void generateApp(MavenProject project, File appDirectory) throws MojoExecutionException {
    this.copyApplicationDependencies(project, new File(appDirectory, "Contents/Java"));
    this.copyNativeExecutable(new File(appDirectory, "Contents/MacOS"));
    if (this.bundleJre != null) {
      this.copyRuntime(new File(this.bundleJre),
          new File(appDirectory, "Contents/PlugIns/Runtime.jre/Contents/"));
    }
    this.generatePlist(project, new File(appDirectory, "Contents/"));
  }

  private void copyApplicationDependencies(MavenProject project, File appJavaDirectory)
      throws MojoExecutionException {
    this.getLog().info("Copy application dependencies to: " + appJavaDirectory.getAbsolutePath());
    try {
      if (StringUtils.isNotEmpty(this.getPlistConfiguration().JVMMainClassName)) {
        this.copyClasspathApplicationDependencies(project, new File(appJavaDirectory, "classpath"));
      } else if (StringUtils.isNotEmpty(this.getPlistConfiguration().JVMMainModuleName)) {
        this.copyModuleApplicationDependencies(project, new File(appJavaDirectory, "modules"));
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Cannot copy dependencies", e);
    }
  }

  private void copyClasspathApplicationDependencies(MavenProject project, File classpathDirectory)
      throws IOException {
    ArtifactRepositoryLayout repositoryLayout = new DefaultRepositoryLayout();
    this.copyClasspathApplicationDependencyArtifact(project.getArtifact(), classpathDirectory,
        repositoryLayout);
    for (Artifact artifact : project.getArtifacts()) {
      this.copyClasspathApplicationDependencyArtifact(artifact, classpathDirectory,
          repositoryLayout);
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
        String targetFileName = StringUtils
            .defaultIfEmpty(this.getPlistConfiguration().CFBundleExecutable, "JavaLauncher");
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
    String iconFileName = this.copyIcon(project, contentsDirectory);
    try {
      File plistFile = new File(contentsDirectory, "Info.plist");
      this.getLog().info("Generating Info.plist");
      FileUtils.write(plistFile, this.getPlistConfiguration().toXmlString(project), "UTF-8");
    } catch (Exception e) {
      throw new MojoExecutionException("Cannot generate Info.plist file", e);
    }
  }

  private String copyIcon(MavenProject project, File contentsDirectory)
      throws MojoExecutionException {
    String iconFileValue = this.getPlistConfiguration().CFBundleIconFile;
    if (StringUtils.isNotEmpty(iconFileValue)) {
      File iconFile = project.getBasedir().toPath().resolve("packaging").resolve(iconFileValue).toFile();
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

  private PlistConfiguration getPlistConfiguration() {
    return this.plistConfiguration;
  }

  private Log getLog() {
    return this.log;
  }
}
