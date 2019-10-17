ARG APP_INSIGHTS_AGENT_VERSION=2.5.1-BETA
FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.2

COPY build/libs/rpa-em-stitching-api.jar lib/AI-Agent.xml /opt/app/

CMD ["rpa-em-stitching-api.jar"]

EXPOSE 8080