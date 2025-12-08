# Bruker Chainguard secure base image, https://sikkerhet.nav.no/docs/sikker-utvikling/baseimages

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jdk:openjdk-25

WORKDIR /app
COPY /app/build/libs/app-all.jar /app/app.jar

ENV LANG='nb_NO.UTF-8' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"
# Kommentar til bruk av XX:ActiveProcessorCount:
# Dette påvirker kode som har logikk basert på JVM-metoden Runtime.getRuntime().availableProcessors()
# Uten limit i Kubernetes returnerer den antall CPU i noden, som kan være mye høyere enn det som er tildelt pod'en.
# Dette kan føre til at applikasjonen prøver å bruke flere tråder enn det som er optimalt for pod'en.
# Nå returnerer metoden det tallet vi angir istedenfor.
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -XX:ActiveProcessorCount=2"

CMD ["java", "-jar", "app.jar"]
