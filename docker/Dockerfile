FROM hub.c.163.com/library/java:8-alpine
#FROM hub.c.163.com/cloudndp/library/openjdk:11
MAINTAINER "uv"<songyw163@163.com>
ADD *.jar stzb.jar
COPY config /config-template
RUN echo "Asia/Shanghai" > /etc/timezone
VOLUME ["/logs", "/config"]
ENTRYPOINT ["java", "-jar", "/stzb.jar"]
