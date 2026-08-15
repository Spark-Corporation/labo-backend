# syntax=docker/dockerfile:1.6
ARG CUSTOM_JDK_VERSION=25

FROM eclipse-temurin:${CUSTOM_JDK_VERSION}-jdk-alpine AS builder
WORKDIR /project

COPY pom.xml mvnw ./
COPY .mvn/ .mvn/
COPY lib/ lib/
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:${CUSTOM_JDK_VERSION}-jdk-alpine AS jre-builder
ARG CUSTOM_JDK_VERSION
ENV JAVA_HOME=/opt/java/openjdk
WORKDIR /project

COPY --from=builder /project/target/lims-backend-*.jar runner.jar

RUN java -Djarmode=layertools -jar runner.jar extract --destination extracted

RUN jdeps --ignore-missing-deps -q --recursive \
      --multi-release ${CUSTOM_JDK_VERSION} \
      --print-module-deps \
      --class-path 'extracted/dependencies/BOOT-INF/lib/*' \
      runner.jar | tr -d '\n' > deps.info

RUN $JAVA_HOME/bin/jlink \
      --verbose \
      --add-modules $(cat deps.info),java.management,java.naming,java.sql,jdk.crypto.ec \
      --strip-debug \
      --no-man-pages \
      --no-header-files \
      --compress=zip-6 \
      --output /optimized-jdk-${CUSTOM_JDK_VERSION}

FROM alpine:3.21 AS runtime
ARG CUSTOM_JDK_VERSION

ENV JAVA_HOME=/opt/java/openjdk \
    PATH="/opt/java/openjdk/bin:${PATH}" \
    LIMS_SERVICE_NAME=lims-backend \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.net.preferIPv4Stack=true"

RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /var/log/lims && chown -R spring:spring /var/log/lims

COPY --from=jre-builder /optimized-jdk-${CUSTOM_JDK_VERSION} /opt/java/openjdk

WORKDIR /app
COPY --from=jre-builder --chown=spring:spring /project/extracted/dependencies/         ./
COPY --from=jre-builder --chown=spring:spring /project/extracted/spring-boot-loader/   ./
COPY --from=jre-builder --chown=spring:spring /project/extracted/snapshot-dependencies/ ./
COPY --from=jre-builder --chown=spring:spring /project/extracted/application/          ./

COPY --chmod=0755 docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

EXPOSE 8080
USER spring
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["org.springframework.boot.loader.launch.JarLauncher"]
