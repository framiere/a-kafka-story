package com.github.framiere;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.github.framiere.Domain.*;

@SpringBootApplication
@EnableJpaRepositories
public class Application implements CommandLineRunner {
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private MemberRepository memberRepository;
    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String bootstrapServers = args.length == 1 ? args[0] : "localhost:9092";
        waitForTopics(bootstrapServers);

        JsonSerde<RandomProducerAction> randomProducerActionSerde = new JsonSerde<>(RandomProducerAction.class);

        StreamsBuilder builder = new StreamsBuilder();
        builder
                .stream(RandomProducerAction.class.getSimpleName(), Consumed.with(Serdes.String(), randomProducerActionSerde))
                .peek((k, action) -> handleProducerAction(action));
        Topology build = builder.build();

        System.out.println(build.describe());

        KafkaStreams kafkaStreams = new KafkaStreams(build, buildProducerProperties(bootstrapServers));
        kafkaStreams.cleanUp();
        kafkaStreams.start();
    }

    private void handleProducerAction(RandomProducerAction action) {
        try {
            System.out.println(action.operation + " " + action.operation.operationMode + " on " + action.clazz);
            String content = OBJECT_MAPPER.writeValueAsString(action.object);
            switch (action.clazz) {
                case "Member":
                    handleMember(action, content);
                    break;
                case "Team":
                    handleTeam(action, content);
                    break;
                case "Address":
                    handleAddress(action, content);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAddress(RandomProducerAction action, String content) throws IOException {
        Address address = OBJECT_MAPPER.readValue(content, Address.class);
        doOperation(action.operation.operationMode, addressRepository, address, address.id);
    }

    private void handleTeam(RandomProducerAction action, String content) throws IOException {
        Team team = OBJECT_MAPPER.readValue(content, Team.class);
        doOperation(action.operation.operationMode, teamRepository, team, team.id);
    }

    private void handleMember(RandomProducerAction action, String content) throws IOException {
        Member member = OBJECT_MAPPER.readValue(content, Member.class);
        doOperation(action.operation.operationMode, memberRepository, member, member.id);
    }

    private void waitForTopics(String bootstrapServers) throws Exception {
        while (true) {
            TimeUnit.SECONDS.sleep(5);
            Properties properties = new Properties();
            properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            AdminClient adminClient = AdminClient.create(properties);
            Set<String> topics = adminClient.listTopics().names().get();
            if (topics.contains(RandomProducerAction.class.getSimpleName())) {
                return;
            }
            System.out.println("Waiting for data");
        }
    }

    public <T, ID> void doOperation(OperationMode operationMode, CrudRepository<T, ID> repo, T object, ID id) {
        switch (operationMode) {
            case UPDATE:
            case INSERT:
                repo.save(object);
                break;
            case DELETE:
                if (repo.existsById(id)) {
                    repo.deleteById(id);
                }
                break;
            default:
                throw new IllegalArgumentException(operationMode + " is not supported");
        }
    }

    private Properties buildProducerProperties(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 3);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "operations-to-mysql");
        properties.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 5 * 1000);
        properties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        properties.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }
}
