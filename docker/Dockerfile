# Dockerfile for universalresolver/driver-did-bid

FROM maven:3-jdk-11 AS build
MAINTAINER guoshijie <guoshijie@caict.ac.cn>

# build driver-did-bid

ADD . /opt/driver-did-bid
RUN cd /opt/driver-did-bid && mvn clean install package -P war -N -DskipTests

FROM jetty:9.4-jre11
MAINTAINER guoshijie <guoshijie@caict.ac.cn>
USER jetty

COPY --from=build --chown=jetty /opt/driver-did-bid/target/*.war /var/lib/jetty/webapps/ROOT.war

# done

EXPOSE 8080
CMD java -Djetty.http.port=8080 -jar /usr/local/jetty/start.jar
