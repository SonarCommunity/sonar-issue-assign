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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.issueassign.exception.ResourceNotFoundException;

import java.util.Collection;
import java.util.HashSet;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResourceFinderTest {

  @Mock
  private SonarIndex sonarIndex;
  @Mock
  private File javaResource;

  private static final String COMPONENT_KEY = "org:project:resource";
  private static final String RESOURCE_KEY = "resource";
  private static final String NOT_FOUND_RESOURCE_KEY = "not found key";
  private static final Integer RESOURCE_ID = 1;
  private Collection<Resource> resources;
  private Resource nonJavaResource;

  @Before
  public void setUp() {
    nonJavaResource = new File(RESOURCE_KEY);
    nonJavaResource.setId(RESOURCE_ID);
    nonJavaResource.setEffectiveKey(RESOURCE_KEY);

    resources = new HashSet<>();
    resources.add(nonJavaResource);
  }

  @Test
  public void testFindWithJavaResource() throws ResourceNotFoundException {

    when(sonarIndex.getResource(isA(File.class))).thenReturn(javaResource);

    final ResourceFinder classUnderTest = new ResourceFinder(sonarIndex);
    final Resource resource = classUnderTest.find(COMPONENT_KEY);

    assertThat(resource).isSameAs(javaResource);
  }

  @Test
  public void testFindWithResourceWithNonJavaComponentKey() throws ResourceNotFoundException {

    when(sonarIndex.getResource(isA(File.class))).thenReturn(null);
    when(sonarIndex.getResources()).thenReturn(resources);

    final ResourceFinder classUnderTest = new ResourceFinder(sonarIndex);
    final Resource resource = classUnderTest.find(RESOURCE_KEY);

    assertThat(resource).isSameAs(nonJavaResource);
  }

  @Test
  public void testFindWithNonJavaResource() throws ResourceNotFoundException {

    when(sonarIndex.getResource(isA(File.class))).thenReturn(null);
    when(sonarIndex.getResources()).thenReturn(resources);

    final ResourceFinder classUnderTest = new ResourceFinder(sonarIndex);
    final Resource resource = classUnderTest.find(RESOURCE_KEY);

    assertThat(resource).isSameAs(nonJavaResource);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testFindWithNonJavaResourceWithNoId() throws ResourceNotFoundException {

    nonJavaResource.setId(null);

    when(sonarIndex.getResource(isA(File.class))).thenReturn(null);
    when(sonarIndex.getResources()).thenReturn(resources);

    final ResourceFinder classUnderTest = new ResourceFinder(sonarIndex);
    final Resource resource = classUnderTest.find(RESOURCE_KEY);

    assertThat(resource).isSameAs(nonJavaResource);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testFindWithNoResourceFound() throws ResourceNotFoundException {

    when(sonarIndex.getResource(isA(File.class))).thenReturn(null);
    when(sonarIndex.getResources()).thenReturn(resources);

    final ResourceFinder classUnderTest = new ResourceFinder(sonarIndex);
    classUnderTest.find(NOT_FOUND_RESOURCE_KEY);
  }
}
