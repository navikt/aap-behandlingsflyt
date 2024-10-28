FROM gcr.io/java21-debian12/sha256:b2875ad56cdbf0b7f7d6b9ae1f9c205f3cec8cdb8791465b1321527f1c1e8559
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"
COPY /app/build/libs/app-all.jar app.jar
CMD ["java", "-XX:MaxRAMPercentage=75.0", "-XX:ActiveProcessorCount=2", "-jar", "app.jar"]

# use -XX:+UseParallelGC when 2 CPUs and 4G RAM.
# use G1GC when using more than 4G RAM and/or more than 2 CPUs
# use -XX:ActiveProcessorCount=2 if less than 1G RAM.
