FROM hseeberger/scala-sbt:11.0.2-oraclelinux7_1.4.4_2.11.12
WORKDIR /app
COPY ./target/scala-2.13/OverlayNetworkSimulator-assembly-0.1.0-SNAPSHOT.jar ./jar/simulation.jar
EXPOSE 8080
ENTRYPOINT java -jar ./jar/simulation.jar