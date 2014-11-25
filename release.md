Documentation
http://central.sonatype.org/pages/ossrh-guide.html
http://central.sonatype.org/pages/apache-maven.html

set version
commit and tag

mvn -P release clean deploy

Nexus (Staging ...)
https://oss.sonatype.org

Staging (does not work)
https://oss.sonatype.org/content/groups/staging/de/jkeylockmanager
mvn -P release nexus-staging:release
mvn -P release nexus-staging:drop
