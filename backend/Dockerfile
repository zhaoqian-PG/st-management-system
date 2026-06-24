# 仅仅使用一个极其轻量、省内存的 Java 8 运行环境
FROM openjdk:8-jre-slim

# 设置工作目录
WORKDIR /app

# 把你在本地打包好的 jar 包直接复制进去
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 纯粹的运行命令，内存消耗极低
CMD ["java", "-jar", "app.jar"]