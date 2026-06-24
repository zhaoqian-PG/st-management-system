# 第一阶段：在 Docker 内部使用 Maven 打包
FROM maven:3.8.8-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# 第二阶段：运行打包好的 Jar 包
FROM eclipse-temurin:8-jdk-jammy
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]git add Dockerfile