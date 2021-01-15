/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.lifecycle;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Container;
import static com.spotify.docker.client.DockerClient.ListContainersParam.allContainers;

import java.util.List;

import static java.lang.String.format;

public class SftpServerContainerLifecycleManger {

  //private static final String DOCKER_IMAGE = "linuxserver/openssh-server:latest";
  private static final String SFTP_SERVER_NAME = "openssh";

  public static Container getContainerByName(String containerName) throws Exception {
    final DockerClient docker = DefaultDockerClient.fromEnv().build();
    final List<Container> containers = docker.listContainers();

    for (Container container : containers) {
      if (container.image().contains(containerName)) {
        return container;
      }
    }

    throw new Exception(format("No container found for name {}", containerName));

    //final ContainerConfig config = ContainerConfig.builder()
    //        .image(DOCKER_IMAGE)
    //        .build();

    //final ContainerCreation creation = docker.createContainer(config, IMAGE_NAME);

  }

  public static Container getContainerByImageId(String imageId) throws Exception {
    final DockerClient docker = DefaultDockerClient.fromEnv().build();
    final List<Container> containers = docker.listContainers(allContainers());

    for (Container container : containers) {
      if (container.id().equals(imageId)) {
        return container;
      }
    }

    throw new Exception(format("No container found for id {}", imageId));

    //final ContainerConfig config = ContainerConfig.builder()
    //        .image(DOCKER_IMAGE)
    //        .build();

    //final ContainerCreation creation = docker.createContainer(config, IMAGE_NAME);

  }

  public static String stopServerContainer(String containerName, int delay) throws Exception {
    final DockerClient docker = DefaultDockerClient.fromEnv().build();
    Container sftpServerContainer = getContainerByName(SFTP_SERVER_NAME);
    docker.stopContainer(sftpServerContainer.id(), delay);
    return sftpServerContainer.id();
  }

  public static void startServerContainer(String containerId) throws Exception {
    final DockerClient docker = DefaultDockerClient.fromEnv().build();
    Container sftpServerContainer = getContainerByImageId(containerId);
    //if (sftpServerContainer.state().equalsIgnoreCase("stopped")) {
    docker.startContainer(containerId);
    //}
  }

}
