/*
 * SonarQube Issue Assign Plugin
 * Copyright (C) 2014 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.plugins.issueassign.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;
import org.sonar.plugins.issueassign.IssueAssignPlugin;
import org.sonar.plugins.issueassign.IssueWrapper;

public class DiagnosticLogger implements BatchExtension {

    private static final Logger LOG = LoggerFactory.getLogger(DiagnosticLogger.class);

    private Settings settings;

    public DiagnosticLogger(final Settings settings) {
        this.settings = settings;
    }

    public void logReason(final IssueWrapper issueWrapper) {
        if (this.isEnabled()) {
            LOG.info("Issue {} won't be auto-assigned.  Reason: {}", issueWrapper.getKey(), issueWrapper.getNoAssignReason());
        }
    }

    public void logAssign(final String issueKey, final String assignee) {
        if (this.isEnabled()) {
            LOG.info("Assigning issue: {} to assignee ", issueKey, assignee);
        }
    }

    private boolean isEnabled() {
        return this.settings.getBoolean(IssueAssignPlugin.PROPERTY_DIAGNOSTIC_LOGGING);
    }
}
