FROM linuxserver/openssh-server

VOLUME /config/test
VOLUME /config/privateDirectory

USER root

RUN touch /config/privateDirectory/file-to-be-renamed.txt
RUN chown root:root /config/privateDirectory

ENV PUID=1000
ENV PGID=1000
ENV TZ=Europe/London
ENV PASSWORD_ACCESS=true
ENV USER_PASSWORD=test
ENV USER_NAME=mule


#EXPOSE ${sftp.listener.port}

CMD ["/init"]