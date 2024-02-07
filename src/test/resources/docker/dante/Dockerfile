FROM bitnami/minideb:bullseye

RUN install_packages \
    dante-server \
    openssl \
    curl

ENV WORKERS 4
ENV CONFIG /etc/sockd.conf

COPY etc/passwd /etc/passwd
COPY etc/passwd /etc/shadow
COPY etc/group /etc/group

COPY dante.conf ${CONFIG}
COPY generate.sh .

RUN adduser --system --no-create-home socks
RUN chmod +x generate.sh
RUN /generate.sh

VOLUME /etc
EXPOSE 1080

CMD danted -N ${WORKERS} -f ${CONFIG}