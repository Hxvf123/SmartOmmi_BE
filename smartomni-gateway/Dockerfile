# Multi-stage build - dung chung cho moi service trong mono-repo
# Build tu ROOT context: docker build -f smartomni-gateway/Dockerfile -t smartomni/gateway .

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy toan bo mono-repo (can parent pom + tat ca module de resolve dependency noi bo nhu smartomni-common)
COPY pom.xml .
COPY smartomni-common ./smartomni-common
COPY smartomni-gateway ./smartomni-gateway

# Chi build module can thiet + cac module no phu thuoc (-am), bo qua test de build nhanh
RUN mvn -q -pl smartomni-gateway -am -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/smartomni-gateway/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
