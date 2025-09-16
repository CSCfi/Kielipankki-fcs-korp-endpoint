# fcs-korp-endpoint
The Korp fcs 2.0 reference endpoint implementation.

## Quick start

Call `mvn clean compile war:war` to create a war file.  
Use `mvn clean package` to do a full build with tests, war, sources and javadoc.

There are though some configurations to change if you want to use it with your own Korp service.

## Kielipankki quick start

Steps to get a local instance running on an apt-based distribution:

```bash
# Dependencies
sudo apt install maven openjdk-21-jdk

# download tomcat11
wget https://archive.apache.org/dist/tomcat/tomcat-11/v11.0.9/bin/apache-tomcat-11.0.9.tar.gz
# and unzip it, removing the tar.gz file
tar -xvzf apache-tomcat-11.0.9.tar.gz && rm apache-tomcat-11.0.9.tar.gz

# download the official migration tool Tomcat 9 -> 11 (javax -> jakarta) 
wget https://archive.apache.org/dist/tomcat/jakartaee-migration/v1.0.9/binaries/jakartaee-migration-1.0.9-bin.tar.gz
# and unzip it, removing the tar.gz file
tar -xvzf jakartaee-migration-1.0.9-bin.tar.gz && rm jakartaee-migration-1.0.9-bin.tar.gz

# Open the project's directory
cd Kielipankki-fcs-korp-endpoint

# Build the WAR file with Maven
sudo mvn clean package -D maven.test.skip=true war:war

# Migrate the war file from javax to jakarta (necessary for Tomcat11)
sudo ../jakartaee-migration-1.0.9/bin/migrate.sh target/fcs-korp-endpoint-0.1-kp-SNAPSHOT.war target/fcs-korp-endpoint-0.1-kp-SNAPSHOT-MIGRATED.war

# Copy WAR into Tomcat webapps
sudo cp ./target/fcs-korp-endpoint-0.1-kp-SNAPSHOT-MIGRATED.war \
   ../apache-tomcat-11.0.9/webapps/fcs-korp.war

#Start Tomcat
sudo ../apache-tomcat-11.0.9/bin/startup.sh

# Test with eg.
curl "localhost:8080/fcs-korp/sru?queryType=fcs&query=%5Bword+%3D+%27bastu%27%5D"
```
