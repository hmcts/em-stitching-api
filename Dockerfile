FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-2.0

ENV APP rpa-em-stitching-api.jar
ENV APPLICATION_TOTAL_MEMORY 1024M
ENV APPLICATION_SIZE_ON_DISK_IN_MB 77

COPY build/libs/$APP /opt/app/

CMD ["$APP"]
