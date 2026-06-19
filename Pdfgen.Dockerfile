FROM ghcr.io/navikt/pdfgenrs:1.0.4
#COPY templates /app/templates
ENV NAIS_APP_NAME=tmp
ENV DEV_MODE=true
ENV SHUTDOWN_DRAIN_SECONDS=0

