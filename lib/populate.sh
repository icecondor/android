# Build OAUTH
svn checkout http://oauth.googlecode.com/svn/code/java/core/ oauth-core-`date +%Y%m%d`-sources
cd oauth-core-...
/usr/local/java/apache-maven-2.2.1/bin/mvn

# Copy jars
cd icecondor-client-android/
cp ../../oauth-core-20100719-sources/core/commons/target/oauth-20100601.jar .
cp ../../oauth-core-20100719-sources/core/consumer/target/oauth-consumer-20100601.jar  .
cp ../../oauth-core-20100719-sources/core/httpclient4/target/oauth-httpclient4-20100601.jar .


