#!/bin/sh
#
# if secret file with runtime parameters exists
if [ -r /etc/secret-volume/mirth.properties ]; then
    cp -f /etc/secret-volume/mirth.properties /opt/mirth-connect/conf/mirth.properties
    echo "Configuring mirth.properties"
fi
# if secret file with runtime parameters exists
if [ -r /etc/secret-volume/keystore.jks ]; then
  mkdir -p /opt/mirth-connect/appdata
  cp -f /etc/secret-volume/keystore.jks /opt/mirth-connect/appdata/keystore.jks
  echo "Configuring keystore.jks"
fi

echo "Launching Mirth Connect Server..."
if [ -r /opt/mirth-connect/newrelic/newrelic.yml ]; then
  java -Duser.timezone="America/Argentina/Buenos_Aires" -javaagent:/opt/mirth-connect/newrelic/newrelic.jar -jar mirth-server-launcher.jar
else
  java -Duser.timezone="America/Argentina/Buenos_Aires" -jar mirth-server-launcher.jar
fi
