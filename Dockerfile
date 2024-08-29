ARG APP_INSIGHTS_AGENT_VERSION=3.4.18

FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/rpa-em-stitching-api.jar /opt/app/

CMD ["rpa-em-stitching-api.jar"]

EXPOSE 8080
