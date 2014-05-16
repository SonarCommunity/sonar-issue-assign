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
package org.sonar.plugins.issueassign.notification;

import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationDispatcherMetadata;
import org.sonar.api.notifications.NotificationManager;

import java.util.Collection;
import java.util.Map;

import static org.sonar.plugins.issueassign.IssueAssignPlugin.NOTIFICATION_TYPE_NEW;

/**
 * This dispatcher means: "notify me when new issues assigned to me are introduced during project scan".
 */
public class MyNewIssuesNotificationDispatcher extends NotificationDispatcher {
    private Logger logger = LoggerFactory.getLogger(getClass());
    public static final String KEY = "MyNewIssues";
    private final NotificationManager manager;

    public MyNewIssuesNotificationDispatcher(NotificationManager manager) {
        super(NOTIFICATION_TYPE_NEW);
        this.manager = manager;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    public static NotificationDispatcherMetadata newMetadata() {
        return NotificationDispatcherMetadata.create(KEY)
                .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
                .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true));
    }

    @Override
    public void dispatch(Notification notification, Context context) {
        logger.debug("Dispatching notification {}", notification);
        String assignee = notification.getFieldValue("assignee");
        if (assignee == null) {
            return;
        }

        String projectKey = notification.getFieldValue("projectKey");
        Multimap<String, NotificationChannel> subscribedRecipients = manager.findNotificationSubscribers(this, projectKey);


        for (Map.Entry<String, Collection<NotificationChannel>> channelsByRecipients : subscribedRecipients.asMap().entrySet()) {
            String userLogin = channelsByRecipients.getKey();
            if (assignee.equals(userLogin)) {
                logger.debug("Sending notification to {} with {} channels.", userLogin, channelsByRecipients.getValue().size());
                for (NotificationChannel channel : channelsByRecipients.getValue()) {
                    logger.debug("Sending to channel {}.", channel);
                    context.addUser(userLogin, channel);
                }
            }
        }

    }

}
