/*
 * SonarQube Issue Assign Plugin
 * Copyright (C) 2014 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.issueassign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.issueassign.exception.ResourceNotFoundException;

import java.util.Collection;

public class ResourceFinder {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceFinder.class);
  private static final int RESOURCE_KEY_INDEX_N0 = 2;
  private SonarIndex sonarIndex;

  public ResourceFinder(final SonarIndex sonarIndex) {
    this.sonarIndex = sonarIndex;
  }

  public Resource find(final String componentKey) throws ResourceNotFoundException {
    final Resource resource = getResource(componentKey);
    if (resource == null) {
      LOG.debug("Cannot lookup resource directly, searching entire index...");
      return searchAllResources(componentKey);
    }
    return resource;
  }

  private Resource getResource(final String componentKey) {
    try {
      final String resourceKey = getResourceKeyFromComponentKey(componentKey);
      final Resource javaResource = this.sonarIndex.getResource(File.create(resourceKey));
      LOG.debug("Found resource with key: [" + javaResource.getKey() + "]");
      return javaResource;
    } catch (final Exception e) {
      return null;
    }
  }

  // component key format: org:project:resourceKey
  private String getResourceKeyFromComponentKey(final String componentKey) {
    return componentKey.split(":")[RESOURCE_KEY_INDEX_N0];
  }

  private Resource searchAllResources(final String componentKey) throws ResourceNotFoundException {
    final Collection<Resource> resources = this.sonarIndex.getResources();

    for (final Resource resource : resources) {
      if (matches(componentKey, resource)) {
        LOG.debug("Found resource for [" + componentKey + "]");
        LOG.debug("Resource class type: [" + resource.getClass().getName() + "]");
        LOG.debug("Resource key: [" + resource.getKey() + "]");
        LOG.debug("Resource id: [" + resource.getId() + "]");
        return resource;
      }
    }

    LOG.warn("No resource found for component [" + componentKey + "]");
    throw new ResourceNotFoundException();
  }

  private boolean matches(final String componentKey, final Resource resource) {
    return resource.getEffectiveKey().equals(componentKey) && resource.getId() != null;
  }
}
