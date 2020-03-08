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
import de.perdian.maven.plugins.macosappbundler.mojo.impl.DmgGenerator;
import de.perdian.maven.plugins.macosappbundler.mojo.model.DmgConfiguration;
import de.perdian.maven.plugins.macosappbundler.mojo.model.PlistConfiguration;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Create all artifacts to publish a Java application as macOS application bundle.
 */

@Mojo(name = "bundle", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PACKAGE)
public class BundleMojo extends AbstractMojo {

  @Component
  private MavenProject project = null;

  @Parameter(required = true)
  private PlistConfiguration plist = null;

  @Parameter
  private String bundleJre = null;

  @Parameter
  private DmgConfiguration dmg = new DmgConfiguration();

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (StringUtils.isEmpty(this.plist.JVMMainClassName) && StringUtils
        .isEmpty(this.plist.JVMMainModuleName)) {
      throw new MojoExecutionException(
          "Neither 'JVMMainClassName' nor 'JVMMainModuleName' have been defined!");
    } else if (StringUtils.isNotEmpty(this.plist.JVMMainClassName) && StringUtils
        .isNotEmpty(this.plist.JVMMainModuleName)) {
      throw new MojoExecutionException(
          "Both 'JVMMainClassName' and 'JVMMainModuleName' have been defined! Make sure to define only one to signalize whether to use a classic classpath application or a moduel application.");
    } else {

      this.plist.CFBundleDisplayName = StringUtils
          .defaultIfEmpty(this.plist.CFBundleDisplayName, this.project.getName());
      this.plist.CFBundleName = StringUtils
          .defaultIfEmpty(this.plist.CFBundleName, this.project.getName());
      this.plist.CFBundleIdentifier = StringUtils.defaultIfEmpty(this.plist.CFBundleIdentifier,
          this.project.getGroupId() + "." + this.project.getArtifactId());
      this.plist.CFBundleShortVersionString = StringUtils
          .defaultIfEmpty(this.plist.CFBundleShortVersionString, this.project.getVersion());
      this.plist.CFBundleExecutable = StringUtils
          .defaultIfEmpty(this.plist.CFBundleExecutable, "JavaLauncher");

      String appName = StringUtils
          .defaultString(this.plist.CFBundleName, this.project.getBuild().getFinalName());
      File targetDirectory = new File(this.project.getBuild().getDirectory());
      File appDirectory = new File(targetDirectory, appName + ".app");
      this.getLog().info("Creating app directory at: " + appDirectory.getAbsolutePath());
      appDirectory.mkdirs();
      AppGenerator appGenerator = new AppGenerator(this.plist, this.bundleJre, this.getLog());
      appGenerator.generateApp(this.project, appDirectory);

      if (this.dmg.generate) {
        File bundleDirectory = new File(targetDirectory, "bundle");
        String dmgFileName;
        if (this.dmg.appendVersion) {
          dmgFileName = appName + "_" + this.project.getVersion() + ".dmg";
        } else if (this.dmg.dmgFileName == null || this.dmg.dmgFileName.isEmpty()) {
          dmgFileName = appName + ".dmg";
        } else {
          dmgFileName = this.dmg.dmgFileName + ".dmg";
        }
        File dmgFile = new File(targetDirectory, dmgFileName);
        DmgGenerator dmgGenerator = new DmgGenerator(this.dmg, appName, this.getLog());
        dmgGenerator.generateDmg(this.project, appDirectory, bundleDirectory, dmgFile);
      }

    }
  }

}
