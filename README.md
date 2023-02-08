# fcs-korp-endpoint
The Korp fcs 2.0 reference endpoint implementation.

## Quick start

Call 'mvn war:war' to create a war file. 

There are though some configurations to change if you want to use it with your own Korp service.

## Kielipankki quick start

Steps to get a local instance running on an apt-based distribution:

```bash
# make sure to be in the branch with our hacks
git checkout kp-dev
# Dependencies
sudo apt install tomcat9 maven openjdk-8-jdk
# You can control the service with eg.
# sudo systemctl start tomcat9.service
# Build package, very old Java version seemed to be necessary for some reason
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/ mvn -D maven.test.skip=true package
# Build war file
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/ mvn -D maven.test.skip=true war:war
# Copy war file to where Tomcat runs things from
sudo cp target/fcs-korp-endpoint-0.1-SNAPSHOT.war /var/lib/tomcat9/webapps/fcs-korp.war


# Can test with eg.
curl "localhost:8080/fcs-korp/sru?queryType=fcs&query=%5Bword+%3D+%27bastu%27%5D"
```
