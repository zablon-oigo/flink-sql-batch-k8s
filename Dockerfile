FROM flink:1.20-java17

USER root

# Add Kafka connector
ADD https://repo1.maven.org/maven2/org/apache/flink/flink-connector-kafka/4.0.0-2.0/flink-connector-kafka-4.0.0-2.0.jar /opt/flink/lib/

# Add JSON format
ADD https://repo1.maven.org/maven2/org/apache/flink/flink-json/2.0.0/flink-json-2.0.0.jar /opt/flink/lib/

# Add Kafka client
ADD https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/3.9.0/kafka-clients-3.9.0.jar /opt/flink/lib/

# Add JDBC connector
ADD https://repo1.maven.org/maven2/org/apache/flink/flink-connector-jdbc/3.4.0-1.20/flink-connector-jdbc-3.4.0-1.20.jar /opt/flink/lib/

# Add PostgreSQL JDBC driver
ADD https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.7/postgresql-42.7.7.jar /opt/flink/lib/

# Add the SQL Runner built by `mvn clean package`
COPY target/flink-sql-runner.jar /opt/flink/usrlib/flink-sql-runner.jar

RUN chown flink:flink /opt/flink/lib/*.jar /opt/flink/usrlib/flink-sql-runner.jar

USER flink