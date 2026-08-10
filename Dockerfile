FROM openjdk:8-jdk-slim

# 决定了下面copy文件进入容器的/app目录下
WORKDIR /app

# 把打包出来的jar包copy进容器，重命名为app.jar（只是内部名称，随便叫）
COPY dining-server/target/*.jar app.jar

 # 重点：启动时指定使用application-pro.yml的配置，并且读取容器内挂载路径下的yml(这里挂载的是application-pro.yml文件，在IDEA中配置挂载路径为/app/config/application-pro.yml)
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=pro","--spring.config.location=file:/app/config/application-pro.yml"]

