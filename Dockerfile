FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app
COPY /app/build/libs/app.jar /app/app.jar
COPY /app/build/libs/runtime-libs/ /app/lib/

ENV LANG='nb_NO.UTF-8' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -XX:ActiveProcessorCount=2"

ENTRYPOINT ["/usr/bin/java"]
CMD ["-cp","/app/app.jar:/app/lib/*","no.nav.aap.behandlingsflyt.AppKt"]

# use -XX:+UseParallelGC when 2 CPUs and 4G RAM.
# use G1GC when using more than 4G RAM and/or more than 2 CPUs
# use -XX:ActiveProcessorCount=2 if less than 1G RAM.
