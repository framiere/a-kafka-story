package com.github.framiere;

import com.github.javafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerSerializer;

import java.security.SecureRandom;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.github.framiere.Domain.*;
import static com.github.framiere.Domain.Gender.FEMALE;
import static com.github.framiere.Domain.Operation.*;
import static java.util.stream.Collectors.toList;

/**
 * Look at the data produced, by example:
 * kafka-console-consumer --bootstrap-server localhost:9092 --topic Team --property print.key=true --key-deserializer org.apache.kafka.common.serialization.IntegerDeserializer --from-beginning
 */
public class RandomProducer {
    private static final Faker faker = new Faker();
    private static final SecureRandom random = new SecureRandom();
    public static final int NB_START_TEAMS = 5;
    public static final int NB_START_MEMBERS = 40;
    private int nbTeams = 0;
    private int nbMembers = 0;

    public static void main(String args[]) throws InterruptedException {
        new RandomProducer().produce(args.length == 1 ? args[0] : "localhost:9092");
    }

    public void produce(String bootstrapServers) throws InterruptedException {
        Producer<Integer, Object> producer = buildProducer(bootstrapServers);

        // create starting teams
        List<Team> teams = buildTeams();
        List<Member> members = buildMembers(teams);
        List<Address> addresses = buildAddresses(members);

        // send to kafka
        teams.stream().forEach(team -> NEW_TEAM.call(producer, team));
        members.stream().forEach(member -> NEW_MEMBER.call(producer, member));
        addresses.stream().forEach(address -> NEW_MEMBER.call(producer, address));

        // create operations
        while (true) {
            Operation operation = randomOperation();
            Team team = randomElement(teams);
            Member member = randomElement(members);
            Address address = addresses.stream().filter(a -> a.id == member.id).findFirst().get();
            switch (operation) {
                case NEW_TEAM:
                    Team newTeam = newTeam();
                    teams.add(newTeam);
                    operation.call(producer, newTeam);
                    break;
                case NEW_MEMBER:
                    nbMembers++;
                    Member newMember = newMember(teams);
                    Address newAddress = newAddress(newMember);
                    members.add(newMember);
                    addresses.add(newAddress);
                    operation.call(producer, newMember);
                    operation.call(producer, newAddress);
                    break;
                case TEAM_NAME_CHANGE:
                    operation.call(producer, team.changeName());
                    break;
                case CHANGE_PHONE:
                    operation.call(producer, address.changePhone());
                    break;
                case CHANGE_ADDRESS_IN_TOWN:
                    operation.call(producer, address.changeAddress());
                    break;
                case CHANGE_CITY:
                    operation.call(producer, address.changeCity());
                    break;
                case CHANGE_COUNTRY:
                    operation.call(producer, address.changeCountry());
                    break;
                case CHANGE_GENDER:
                    operation.call(producer, member
                            .withFirstname(faker.name().firstName())
                            .withGender(randomEnum(Gender.class)));
                    break;
                case DELETE_MEMBER:
                    operation.call(producer, member);
                    members.remove(member);
                    break;
                case DELETE_TEAM:
                    operation.call(producer, team);
                    teams.remove(team);
                    break;
                case CHANGE_TEAM:
                    operation.call(producer, member.withTeam(team));
                    break;
                case CHANGE_ROLE:
                    operation.call(producer, member.withRole(randomEnum(Role.class)));
                    break;
                case NEW_MARITAL_STATUS:
                    MaritalStatus newMaritalStatus = randomEnum(MaritalStatus.class);
                    Member memberWithNewMaritalStatus = member.withMaritalStatus(newMaritalStatus);
                    switch (newMaritalStatus) {
                        case DIVORCED:
                        case MARRIED:
                            operation.call(producer, address.changeAddress());
                            if (memberWithNewMaritalStatus.gender == FEMALE) {
                                operation.call(producer, memberWithNewMaritalStatus.withLastname(faker.name().lastName()));
                            } else {
                                operation.call(producer, memberWithNewMaritalStatus);
                            }
                            break;
                        default:
                            operation.call(producer, member.withMaritalStatus(newMaritalStatus));
                            break;
                    }
                    break;
                case ANNIVERSARY:
                    operation.call(producer, member.withAge(member.age + 1));
                    break;
                case NO_OP:
                    break;
                default:
                    throw new IllegalArgumentException(operation + " is not supported");
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    private List<Address> buildAddresses(List<Member> members) {
        return IntStream.range(1, NB_START_MEMBERS)
                .mapToObj(i -> newAddress(members.get(i - 1)).withCountry("USA"))
                .collect(toList());
    }

    private List<Member> buildMembers(List<Team> teams) {
        return IntStream.range(1, NB_START_MEMBERS)
                .mapToObj(i -> newMember(teams))
                .collect(toList());
    }

    private List<Team> buildTeams() {
        return IntStream.range(1, NB_START_TEAMS)
                .mapToObj(i -> newTeam())
                .collect(toList());
    }

    private Operation randomOperation() {
        Operation operation = randomEnum(Operation.class);
        return operation.fire() ? operation : NO_OP;
    }

    private Member newMember(List<Team> teams) {
        return new Member()
                .withId(nbMembers++)
                .withFirstname(faker.name().firstName())
                .withLastname(faker.name().lastName())
                .withGender(randomEnum(Gender.class))
                .withRole(randomEnum(Role.class))
                .withMaritalStatus(randomEnum(MaritalStatus.class))
                .withAge(random.nextInt(50))
                .withPhone(faker.phoneNumber().phoneNumber())
                .withTeam(randomElement(teams));
    }

    private Address newAddress(Member member) {
        return new Address()
                .withId(member.id)
                .changeCountry();
    }

    private Team newTeam() {
        return new Team()
                .withId(nbTeams++)
                .changeName();
    }

    private <T extends Enum<?>> T randomEnum(Class<T> clazz) {
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

    private <T> T randomElement(List<T> l) {
        return l.get(random.nextInt(l.size()));
    }

    private Producer<Integer, Object> buildProducer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 10);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        return new KafkaProducer<>(props);
    }
}