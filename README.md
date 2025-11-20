# fcs-korp-endpoint
The Korp fcs 2.0 reference endpoint implementation.

## Quick start

Call `mvn clean compile war:war` to create a war file.  
Use `mvn clean package` to do a full build with tests, war, sources and javadoc.

There are though some configurations to change if you want to use it with your own Korp service.

## Kielipankki quick start

Steps to get a local instance running on an apt-based distribution:

```bash
# Install dependencies
sudo apt install maven openjdk-21-jdk tomcat10 tomcat-jakartaee-migration

# Open the project's directory
cd Kielipankki-fcs-korp-endpoint

# Build the WAR file with Maven
mvn clean package -D maven.test.skip=true war:war
# OR run tests with test config:
mvn clean package -Dconfig.file=test-config.properties war:war

# Migrate the war file from javax to jakarta (necessary for Tomcat11)
/usr/bin/javax2jakarta target/fcs-korp-endpoint-1.0-kp.war target/fcs-korp-endpoint-1.0-kp-MIGRATED.war

# Copy WAR into Tomcat webapps
cp ./target/fcs-korp-endpoint-1.0-kp-MIGRATED.war /var/lib/tomcat10/webapps/fcs-korp.war

#Start Tomcat
sudo systemctl start tomcat10

# Test with eg.
curl "localhost:8080/fcs-korp/sru?queryType=fcs&query=%5Bword+%3D+%27bastu%27%5D"
```
