package org.codehaus.mojo.versions;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.versions.api.ArtifactVersions;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;

import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Replaces any version with an exact version. Custom SAILIS extension.
 *
 * @author sbk
 * @goal use-exact-version
 * @requiresProject true
 * @requiresDirectInvocation true
 * @since 2.3-ruiboy
 */
public class UseExactVersionMojo
        extends AbstractVersionsDependencyUpdaterMojo
{
  /**
   * Exact version to set. It's just set; no fancy comparison rules are invoked.
   *
   * @parameter property="exactVersion"
   * @required
   */
  protected String exactVersion;

  /**
   * @param pom
   *          the pom to update.
   * @throws org.apache.maven.plugin.MojoExecutionException
   *           when things go wrong
   * @throws org.apache.maven.plugin.MojoFailureException
   *           when things go wrong in a very bad way
   * @throws javax.xml.stream.XMLStreamException
   *           when things go wrong with XML streaming
   * @see AbstractVersionsUpdaterMojo#update(org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader)
   */
  @Override
  protected void update(ModifiedPomXMLEventReader pom)
          throws MojoExecutionException, MojoFailureException, XMLStreamException
  {
    // @required annotation does not check for blank
    exactVersion = exactVersion.trim();
    if (exactVersion.length() == 0)
    {
      throw new IllegalArgumentException("exactVersion must not be blank");
    }

    try
    {
      if (getProject().getDependencyManagement() != null && isProcessingDependencyManagement())
      {
        useExactVersion(pom, getProject().getDependencyManagement().getDependencies());
      }
      if (isProcessingDependencies())
      {
        useExactVersion(pom, getProject().getDependencies());
      }
    }
    catch (ArtifactMetadataRetrievalException e)
    {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void useExactVersion(ModifiedPomXMLEventReader pom, Collection dependencies)
          throws XMLStreamException, MojoExecutionException, ArtifactMetadataRetrievalException
  {
    Iterator i = dependencies.iterator();

    while (i.hasNext())
    {
      Dependency dep = (Dependency) i.next();

      if (isExcludeReactor() && isProducedByReactor(dep))
      {
        getLog().info("Ignoring reactor dependency: " + toString(dep));
        continue;
      }

      String version = dep.getVersion();
      Artifact artifact = this.toArtifact(dep);
      if (!isIncluded(artifact))
      {
        continue;
      }

      if (PomHelper.setDependencyVersion(pom, dep.getGroupId(), dep.getArtifactId(), version, exactVersion))
      {
        getLog().info("Updated " + toString(dep) + " to version " + exactVersion);
      }
    }
  }

}