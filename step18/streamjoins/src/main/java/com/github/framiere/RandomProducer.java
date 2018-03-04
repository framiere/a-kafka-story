package com.github.framiere;

import com.github.javafaker.Faker;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;

import java.security.SecureRandom;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.github.framiere.Domain.*;
import static com.github.framiere.Domain.Gender.FEMALE;
import static com.github.framiere.RandomProducer.Operation.*;
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
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 10);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        Producer<Integer, HasId> producer = new KafkaProducer<>(props);

        List<Team> teams = buildTeams(producer);
        List<Member> members = buildMembers(producer, teams);
        List<Address> addresses = buildAddresses(producer, members);

        // send to kafka
        teams.stream().forEach(team -> upsert(NEW_TEAM, producer, team));
        members.stream().forEach(member -> upsert(NEW_MEMBER, producer, member));
        addresses.stream().forEach(address -> upsert(NEW_MEMBER, producer, address));

        while (true) {
            Operation operation = randomOperation();
            Team randomTeam = randomElement(teams);
            Member randomMember = randomElement(members);
            Address randomAddress = addresses.stream().filter(a -> a.id == randomMember.id).findFirst().get();
            switch (operation) {
                case NEW_TEAM:
                    Team newTeam = newTeam();
                    teams.add(newTeam);
                    upsert(operation, producer, newTeam);
                    break;
                case NEW_MEMBER:
                    nbMembers++;
                    Member newMember = newMember(teams);
                    Address newAddress = newAddress(newMember);
                    members.add(newMember);
                    addresses.add(newAddress);
                    upsert(operation, producer, newMember);
                    upsert(operation, producer, newAddress);
                    break;
                case TEAM_NAME_CHANGE:
                    upsert(operation, producer, randomTeam.changeName());
                    break;
                case CHANGE_PHONE:
                    upsert(operation, producer, randomAddress.changePhone());
                    break;
                case CHANGE_ADDRESS_IN_TOWN:
                    upsert(operation, producer, randomAddress.changeAddress());
                    break;
                case CHANGE_CITY:
                    upsert(operation, producer, randomAddress.changeCity());
                    break;
                case CHANGE_COUNTRY:
                    upsert(operation, producer, randomAddress.changeCountry());
                    break;
                case CHANGE_GENDER:
                    upsert(operation, producer,
                            randomMember
                                    .withFirstname(faker.name().firstName())
                                    .withGender(randomEnum(Gender.class)));
                    break;
                case DELETE_MEMBER:
                    delete(operation, producer, randomMember);
                    members.remove(randomMember);
                    break;
                case DELETE_TEAM:
                    delete(operation, producer, randomTeam);
                    teams.remove(randomTeam);
                    break;
                case CHANGE_TEAM:
                    delete(operation, producer,
                            randomMember.withTeam(randomTeam));
                    break;
                case CHANGE_ROLE:
                    delete(operation, producer,
                            randomMember.withRole(randomEnum(Role.class)));
                    break;
                case NEW_MARITAL_STATUS:
                    MaritalStatus newMaritalStatus = randomEnum(MaritalStatus.class);
                    if (newMaritalStatus != randomMember.maritalStatus) {
                        Member memberWithNewMaritalStatus = randomMember.withMaritalStatus(newMaritalStatus);
                        switch (newMaritalStatus) {
                            case DIVORCED:
                            case MARRIED:
                                upsert(operation, producer, randomAddress.changeAddress());
                                if (memberWithNewMaritalStatus.gender == FEMALE) {
                                    upsert(operation, producer, memberWithNewMaritalStatus.withLastname(faker.name().lastName()));
                                } else {
                                    upsert(operation, producer, memberWithNewMaritalStatus);
                                }
                                break;
                            default:
                                upsert(operation, producer, randomMember.withMaritalStatus(newMaritalStatus));
                                break;
                        }
                    }
                    break;
                case ANNIVERSARY:
                    upsert(operation, producer, randomMember.withAge(randomMember.age + 1));
                    break;
                case NO_OP:
                    break;
                default:
                    throw new IllegalArgumentException(operation + " is not supported");
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    private List<Address> buildAddresses(Producer<Integer, HasId> producer, List<Member> members) {
        return IntStream.range(1, NB_START_MEMBERS)
                .mapToObj(i -> newAddress(members.get(i - 1)).withCountry("USA"))
                .collect(toList());
    }

    private List<Member> buildMembers(Producer<Integer, HasId> producer, List<Team> teams) {
        return IntStream.range(1, NB_START_MEMBERS)
                .mapToObj(i -> newMember(teams))
                .collect(toList());
    }

    private List<Team> buildTeams(Producer<Integer, HasId> producer) {
        return IntStream.range(1, NB_START_TEAMS)
                .mapToObj(i -> newTeam())
                .collect(toList());
    }

    private Operation randomOperation() {
        Operation operation = randomEnum(Operation.class);
        return operation.fire() ? operation : NO_OP;
    }

    @AllArgsConstructor
    enum Operation {
        NEW_TEAM(8),
        NEW_MEMBER(15),
        TEAM_NAME_CHANGE(20),
        DELETE_TEAM(3),
        DELETE_MEMBER(4),
        NEW_MARITAL_STATUS(5),
        CHANGE_PHONE(2),
        CHANGE_ADDRESS_IN_TOWN(5),
        CHANGE_CITY(4),
        CHANGE_COUNTRY(1),
        CHANGE_GENDER(1),
        CHANGE_TEAM(5),
        CHANGE_ROLE(11),
        ANNIVERSARY(2),
        NO_OP(100);
        int chance;

        boolean fire() {
            return random.nextInt(100) <= chance;
        }
    }

    private void upsert(Operation operation, Producer<Integer, HasId> producer, HasId object) {
        System.out.println(operation + " " + object);
        producer.send(new ProducerRecord<>(object.getClass().getSimpleName(), object.getId(), object));
    }

    private void delete(Operation operation, Producer<Integer, HasId> producer, HasId object) {
        System.out.println(operation + " " + object);
        producer.send(new ProducerRecord<>(object.getClass().getSimpleName(), object.getId(), null));
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
}