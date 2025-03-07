package es.upm.miw.configuration;

import es.upm.miw.OrderConsumerService;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Log4j2
@Configuration
public class KafkaStreamsConfig {

    public static final String TOPIC_ORDER_OUT = "order-out";
    public static final int PARTITIONS = 1;
    public static final short REPLICATION_FACTOR = 1;

    private final OrderConsumerService orderConsumerService;
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.streams.application-id}")
    private String applicationId;

    @Autowired
    public KafkaStreamsConfig(OrderConsumerService orderConsumerService) {
        this.orderConsumerService = orderConsumerService;
    }

    @Bean
    public KafkaStreams kafkaStreams() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        createTopicsIfNotExists(bootstrapServers, TOPIC_ORDER_OUT);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> stream = builder.stream(TOPIC_ORDER_OUT);
        stream.foreach(orderConsumerService::processMessage);

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        return streams;
    }

    private void createTopicsIfNotExists(String bootstrapServers, String topicName) {
        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            Set<String> existingTopics = adminClient.listTopics().names().get();
            if (existingTopics.contains(topicName)) {
                log.debug("El tópico '{}' ya existe.", topicName);
            } else {
                NewTopic newTopic = new NewTopic(topicName, PARTITIONS, REPLICATION_FACTOR);
                adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
                log.info("Tópico '{}' creado correctamente.", topicName);
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error("❌ Error al gestionar el tópico: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}

