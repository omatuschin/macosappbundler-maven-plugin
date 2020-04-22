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

import de.perdian.maven.plugins.macosappbundler.mojo.impl.DmgGenerator;
import de.perdian.maven.plugins.macosappbundler.mojo.model.DmgConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Create all artifacts to publish a Java application as macOS application bundle.
 */

@Mojo(name = "diskimage", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.INSTALL)
public class DmgGenerateMojo extends AbstractMojo {

    @Component
    private MavenProject project = null;

    @Parameter
    private String bundleName = null;

    @Parameter
    private DmgConfiguration dmg = new DmgConfiguration();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        String appName = StringUtils.defaultString(this.bundleName, this.project.getBuild().getFinalName());
        File targetDirectory = new File(this.project.getBuild().getDirectory());
        File appDirectory = new File(targetDirectory, appName + ".app");

        File bundleDirectory = new File(targetDirectory, "bundle");

        String dmgFileName;
        if (this.dmg.dmgFileName != null) {
            dmgFileName = this.dmg.dmgFileName;
        } else {
            dmgFileName = appName;
        }

        if (this.dmg.appendVersion) {
            dmgFileName = dmgFileName + "_" + this.project.getVersion();
        }

        dmgFileName = dmgFileName + ".dmg";

        File dmgFile = new File(targetDirectory, dmgFileName);
        DmgGenerator dmgGenerator = new DmgGenerator(this.dmg, appName, this.getLog());
        dmgGenerator.generateDmg(this.project, appDirectory, bundleDirectory, dmgFile);

    }

}
