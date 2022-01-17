ARG APP_INSIGHTS_AGENT_VERSION=2.6.4
FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY build/libs/rpa-em-stitching-api.jar lib/applicationinsights-agent-2.5.1.jar lib/AI-Agent.xml /opt/app/

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" wget -q --spider http://localhost:8080/health || exit 1

CMD ["rpa-em-stitching-api.jar"]

EXPOSE 8080