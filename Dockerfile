FROM java

ENV MIRTH_CONNECT_VERSION 3.7.0.b2399
ENV MIRTH_DOWNLOAD_URL https://s3.amazonaws.com/downloads.mirthcorp.com/connect/

# Mirth Connect is run with user `connect`, uid = 1000
# If you bind mount a volume from the host or a data container,
# ensure you use the same uid
#RUN useradd -u 1000 mirth

# grab gosu for easy step-down from root
RUN gpg --keyserver pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates wget && rm -rf /var/lib/apt/lists/* \
	&& wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/1.2/gosu-$(dpkg --print-architecture)" \
	&& wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/1.2/gosu-$(dpkg --print-architecture).asc" \
	&& gpg --verify /usr/local/bin/gosu.asc \
	&& rm /usr/local/bin/gosu.asc \
	&& chmod +x /usr/local/bin/gosu

VOLUME /opt/mirth-connect/appdata

RUN \
  cd /tmp && \
	wget $MIRTH_DOWNLOAD_URL$MIRTH_CONNECT_VERSION/mirthconnect-$MIRTH_CONNECT_VERSION-unix.tar.gz && \
  tar xvzf mirthconnect-$MIRTH_CONNECT_VERSION-unix.tar.gz && \
  rm -f mirthconnect-$MIRTH_CONNECT_VERSION-unix.tar.gz && \
  mv Mirth\ Connect/* /opt/mirth-connect/

#	&& \ chown -R mirth /opt/mirth-connect

WORKDIR /opt/mirth-connect

# configure FHIR connector extensions use port 9443
COPY fhir-3.7.0.b1046.zip /tmp
RUN unzip /tmp/fhir-3.7.0.b1046.zip -d /opt/mirth-connect/extensions && \
    rm /tmp/fhir-3.7.0.b1046.zip

EXPOSE 8080 8443 9443

COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]

CMD ["java", "-jar", "mirth-server-launcher.jar"]
