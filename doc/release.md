# Deploy

## Prepare
set version in pom.xml
commit and tag

## Deploy to sonatype nexus
mvn -P release clean deploy


## check

### check sonatype nexus (staging is not active - releases are deployed directly)
https://oss.sonatype.org

### check central (~ 10 min later)
http://repo1.maven.org/maven2/de/jkeylockmanager/jkeylockmanager/



# Documentation
http://central.sonatype.org/pages/ossrh-guide.html
http://central.sonatype.org/pages/apache-maven.html


![](https://cdn.rawgit.com/mojgh/JKeyLockManager/master/doc/lock-chart.svg)