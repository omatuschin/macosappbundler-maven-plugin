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
package de.perdian.maven.plugins.macosappbundler.mojo.model;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public class PlistConfiguration {

  @Parameter
  public String CFBundleIconFile;

  @Parameter
  public String CFBundleIdentifier = null;

  @Parameter
  public String CFBundleDisplayName = null;

  @Parameter
  public String CFBundleName = null;

  @Parameter
  public String CFBundleShortVersionString = null;

  @Parameter
  public String CFBundleExecutable = null;

  @Parameter
  public String JVMMainClassName = null;

  @Parameter
  public String JVMMainModuleName = null;

  public String toXmlString(MavenProject project) throws Exception {

    StringBuilder plistBuilder = new StringBuilder();

    List<String> plistLines = Files.readAllLines(
        project.getBasedir().toPath().resolve(Paths.get("packaging")).resolve("Info.plist"));
    for (String plistLine : plistLines) {
      while (plistLine.contains("${") && plistLine.contains("}")
          && plistLine.indexOf("}") > plistLine.indexOf("${")) {
        plistBuilder.append(plistLine, 0, plistLine.indexOf("${"));
        String variableName = plistLine
            .substring(plistLine.indexOf("${") + 2, plistLine.indexOf("}"));
        Field field = getClass().getDeclaredField(variableName);
        if (field != null) {
          plistBuilder.append(field.get(this));
        }
        plistLine = plistLine.substring(plistLine.indexOf("}") + 1);
      }
      plistBuilder.append(plistLine);
    }

    return plistBuilder.toString();

  }
}
