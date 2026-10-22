package com.np3tech.hoopmind.ml.spark;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnSpark
@Slf4j
public class SparkConfig {

    @Value("${spark.master:local[*]}")
    private String sparkMaster;

    @Value("${spark.app-name:NBA-Prediction-Analytics}")
    private String appName;

    @Value("${spark.executor.memory:2g}")
    private String executorMemory;

    @Value("${spark.driver.memory:1g}")
    private String driverMemory;

    @Value("${spark.sql.shuffle.partitions:10}")
    private String shufflePartitions;

    @Value("${spark.driver.port:8085}")
    private String driverPort;

    /** Nom d'hôte sous lequel les exécuteurs joignent le driver (le nom du service en Docker). */
    @Value("${spark.driver.host:localhost}")
    private String driverHost;

    @Bean(destroyMethod = "stop")
    public SparkSession sparkSession() {
        log.info("Initializing Spark Session with master: {}", sparkMaster);

        // Flags JVM nécessaires à Spark sur Java 17+
        String javaOptions = String.join(" ",
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
                "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.nio=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
                "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
                "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
                "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
                "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
                "--add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED"
        );

        SparkConf sparkConf = new SparkConf()
                .setAppName(appName)
                .setMaster(sparkMaster)
                .set("spark.executor.memory", executorMemory)
                .set("spark.driver.memory", driverMemory)
                .set("spark.sql.shuffle.partitions", shufflePartitions)
                .set("spark.sql.adaptive.enabled", "true")
                .set("spark.sql.adaptive.coalescePartitions.enabled", "true")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryoserializer.buffer.max", "512m")
                .set("spark.sql.warehouse.dir", "spark-warehouse")
                // Pas d'interface web Spark (port 4040) : rien ne la consulte et elle ne doit pas être exposée.
                .set("spark.ui.enabled", "false")
                .set("spark.driver.host", driverHost)
                .set("spark.driver.port", driverPort)
                // Ajouter les options Java pour driver et executors
                .set("spark.driver.extraJavaOptions", javaOptions)
                .set("spark.executor.extraJavaOptions", javaOptions);

        try {
            SparkSession spark = SparkSession.builder()
                    .config(sparkConf)
                    .getOrCreate();

            log.info("✅ Spark Session initialized successfully");
            log.info("Spark version: {}", spark.version());
            log.info("Spark master: {}", spark.sparkContext().master());

            return spark;
        } catch (Exception e) {
            log.error("❌ Failed to initialize Spark Session", e);
            throw new RuntimeException("Could not initialize Spark Session", e);
        }
    }
}
