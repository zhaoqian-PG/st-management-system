# 🛠️ 使用专门针对 Java 8 (OpenJDK 8) 的官方 Maven 镜像
FROM maven:3.8.4-openjdk-8

# 设置容器内的工作目录
WORKDIR /app

# 把当前 backend 目录下的所有文件复制到容器里
COPY . .

# 在容器内部执行 Java 8 的编译打包
RUN mvn clean package -DskipTests

# 声明暴露后端端口（Spring Boot 默认是 8080）
EXPOSE 8080

# 启动命令：运行 Java 8 打包出来的 jar 包
CMD ["sh", "-c", "java -jar target/*.jar"]