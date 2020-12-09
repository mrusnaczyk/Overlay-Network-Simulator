FROM bigtruedata/sbt
WORKDIR /app
COPY ./src ./
COPY ./project ./
COPY ./.bloop ./
COPY build.sbt ./
RUN sbt compile
ENTRYPOINT sbt run