ARG APP_INSIGHTS_AGENT_VERSION=2.6.4
FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4

COPY build/libs/rpa-em-stitching-api.jar /opt/app/
COPY lib/AI-Agent.xml /opt/app/

CMD ["rpa-em-stitching-api.jar"]

EXPOSE 8080