FROM eclipse-temurin:21-jdk

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        maven \
        unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY pom.xml ./

COPY conf/cxplain_eval_50per.toml ./conf/cxplain_eval_50per.toml
COPY src ./src
COPY lib/configurator-1.0.1-alpha-43.jar ./lib/configurator-1.0.1-alpha-43.jar
COPY data/confs ./data/confs
COPY data/fms ./data/fms
COPY data/sconfs ./data/sconfs
RUN mkdir ./data/results

RUN mvn validate
RUN mvn install
RUN mvn package

RUN mv ./target/cxplain_eval-jar-with-dependencies.jar app.jar

RUN unzip ./data/sconfs/REAL-FM-7.zip -d ./data/sconfs/
RUN unzip ./data/sconfs/arcade-game.zip -d ./data/sconfs/
RUN unzip ./data/sconfs/fqa.zip -d ./data/sconfs/
RUN unzip ./data/sconfs/ubuntu.zip -d ./data/sconfs/
RUN unzip ./data/sconfs/windows8.zip -d ./data/sconfs/

RUN java -jar app.jar -cfg ./conf/cxplain_eval_50per.toml