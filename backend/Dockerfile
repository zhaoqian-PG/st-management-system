# 🛠️ 换成亚马逊长期维护的、绝对健在的 Amazon Corretto Java 8 运行镜像
FROM amazoncorretto:8-alpine-jre

# 设置工作目录
WORKDIR /app

# 把你在本地打包好的 jar 包直接复制进去
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 纯粹的运行命令，内存消耗极低
CMD ["java", "-jar", "app.jar"]