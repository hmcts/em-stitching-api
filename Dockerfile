FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-2.0.1

COPY build/libs/rpa-em-stitching-api.jar /opt/app/

CMD ["rpa-em-stitching-api.jar"]

EXPOSE 8080