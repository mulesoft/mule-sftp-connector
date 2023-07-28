/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.it;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.lang.String.format;

public class SftpServerContainerLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpServerContainerLifecycleManager.class);

    public static Container getContainerByName(String containerName) throws Exception {
        final DockerClient docker = DefaultDockerClient.fromEnv().build();
        final List<Container> containers = docker.listContainers();

        for (Container container : containers) {
            if (container.image().contains(containerName)) {
                return container;
            }
        }

        throw new Exception(format("No container found for name {}", containerName));
    }


    public static String stopServerContainer(String containerName, int delay) {
        String containerId = "";
        try {
            final DockerClient docker = DefaultDockerClient.fromEnv().build();
            Container sftpServerContainer = getContainerByName(containerName);
            docker.stopContainer(sftpServerContainer.id(), delay);
            LOGGER.info(String.format("STOPPING DOCKER CONTAINER %s", sftpServerContainer.id()));
            containerId = sftpServerContainer.id();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return containerId;
    }

    public static void startServerContainer(String containerId) throws Exception {
        try {
            LOGGER.info(String.format("STARTING DOCKER CONTAINER %s", containerId));
            final DockerClient docker = DefaultDockerClient.fromEnv().build();
            docker.startContainer(containerId);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }

}