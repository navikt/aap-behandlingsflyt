FROM eclipse-temurin:23-jre-alpine
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"
COPY /app/build/libs/app-all.jar app.jar
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=2 $JDK_JAVA_OPTIONS"
CMD ["app.jar"]

# use -XX:+UseParallelGC when 2 CPUs and 4G RAM.
# use G1GC when using more than 4G RAM and/or more than 2 CPUs
# use -XX:ActiveProcessorCount=2 if less than 1G RAM.
