FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.1

COPY build/libs/rpa-em-stitching-api.jar /opt/app/

CMD ["rpa-em-stitching-api.jar"]

EXPOSE 8080