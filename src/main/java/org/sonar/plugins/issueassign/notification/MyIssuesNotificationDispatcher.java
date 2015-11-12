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
package org.sonar.plugins.issueassign.notification;

import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.NotificationManager;

import java.util.Collection;
import java.util.Map;

/**
 * Parent notification dispatcher for my-new-issues and my-changed-issues.
 */
public class MyIssuesNotificationDispatcher extends NotificationDispatcher {
  private final NotificationManager manager;
  private static final Logger LOG = LoggerFactory.getLogger(MyIssuesNotificationDispatcher.class);

  public MyIssuesNotificationDispatcher(String notificationType, NotificationManager manager) {
    super(notificationType);
    this.manager = manager;
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    LOG.debug("Dispatching notification {}", notification);
    String assignee = notification.getFieldValue("assignee");
    if (assignee == null) {
      return;
    }

    String projectKey = notification.getFieldValue("projectKey");
    Multimap<String, NotificationChannel> subscribedRecipients = manager.findNotificationSubscribers(this, projectKey);

    for (Map.Entry<String, Collection<NotificationChannel>> channelsByRecipients : subscribedRecipients.asMap().entrySet()) {
      String userLogin = channelsByRecipients.getKey();
      if (assignee.equals(userLogin)) {
        LOG.debug("Sending notification to {} with {} channels.", userLogin, channelsByRecipients.getValue().size());
        for (NotificationChannel channel : channelsByRecipients.getValue()) {
          LOG.debug("Sending to channel {}.", channel);
          context.addUser(userLogin, channel);
        }
      }
    }

  }
}
