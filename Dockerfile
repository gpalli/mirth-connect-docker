FROM openjdk:8-jre

ENV MIRTH_CONNECT_VERSION 3.7.0.b2399
ENV MIRTH_DOWNLOAD_URL https://s3.amazonaws.com/downloads.mirthcorp.com/connect/

RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates wget && \
    rm -rf /var/lib/apt/lists/*

# download and configure mirth product
RUN \
  cd /tmp && \
	wget $MIRTH_DOWNLOAD_URL$MIRTH_CONNECT_VERSION/mirthconnect-$MIRTH_CONNECT_VERSION-unix.tar.gz && \
  tar xvzf mirthconnect-$MIRTH_CONNECT_VERSION-unix.tar.gz && \
  mkdir -p /opt/mirth-connect && \
  mv Mirth\ Connect/* /opt/mirth-connect/ && \
  rm -f mirthconnect-$MIRTH_CONNECT_VERSION-unix.tar.gz

WORKDIR /opt/mirth-connect

# configure mysql connector
RUN rm -fr /opt/mirth-connect/server-lib/database/mysql-connector-java-*.jar
COPY mysql-connector-java-5.1.47-bin.jar /opt/mirth-connect/server-lib/database

# configure FHIR connector extensions
COPY fhir-3.7.0.b1046.zip /tmp
RUN unzip /tmp/fhir-3.7.0.b1046.zip -d /opt/mirth-connect/extensions && \
    rm /tmp/fhir-3.7.0.b1046.zip

# configuro newrelic
COPY newrelic-java-5.0.0.zip /tmp
RUN unzip /tmp/newrelic-java-5.0.0.zip -d /opt/mirth-connect/ && \
    rm /tmp/newrelic-java-5.0.0.zip && \
    rm /opt/mirth-connect/newrelic/newrelic.yml

# Configuro LDAP client sinergis
RUN mkdir -p /opt/mirth-connect/ldap-lib
COPY LdapUtil.jar /opt/mirth-connect/ldap-lib

# configure launcher
COPY run.sh .
RUN chmod +x run.sh

EXPOSE 8080 8443

CMD ["./run.sh"]
