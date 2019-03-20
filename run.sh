#!/bin/sh
#
# if secret file with runtime parameters exists
if [ -r /etc/secret-volume/mirth.properties ]; then
    cp -f /etc/secret-volume/mirth.properties /opt/mirth-connect/conf/mirth.properties
fi
# if secret file with runtime parameters exists
if [ -r /etc/secret-volume/keystore.jks ]; then
  cp -f /etc/secret-volume/keystore.jks /opt/mirth-connect/appdata/keystore.jks
fi

java -Duser.timezone="America/Argentina/Buenos_Aires" -jar mirth-server-launcher.jar
