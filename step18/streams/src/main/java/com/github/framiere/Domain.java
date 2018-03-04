package com.github.framiere;

import com.github.javafaker.Faker;
import lombok.*;
import lombok.experimental.Wither;

public class Domain {
    private static final Faker faker = new Faker();

    public interface HasId {
        int getId();
    }

    @Data
    @EqualsAndHashCode(of = "id")
    @NoArgsConstructor
    @AllArgsConstructor
    @Wither
    public static class Member implements HasId {
        public int id;
        public String firstname;
        public String lastname;
        public Gender gender;
        public MaritalStatus maritalStatus;
        public int team_id;
        public int age;
        public Role role;
    }

    @Data
    @EqualsAndHashCode(of = "id")
    @NoArgsConstructor
    @AllArgsConstructor
    @Wither
    public static class Address implements HasId {
        public int id;
        public String streetName;
        public String streetAddress;
        public String city;
        public String state;
        public String country;

        public Address changeAddress() {
            return withStreetName(faker.address().streetName())
                    .withStreetAddress(faker.address().streetAddress());
        }

        public Address changeCity() {
            return changeAddress()
                    .withCity(faker.address().city());
        }

        public Address changeState() {
            return changeCity()
                    .withState(faker.address().state());
        }

        public Address changeCountry() {
            return changeState()
                    .withCountry(faker.address().country());
        }

    }

    public enum MaritalStatus {
        MARRIED,
        SINGLE,
        DIVORCED,
        WIDOWED
    }

    public enum Role {
        DEVELOPER,
        QA,
        ARCHITECT,
        MANAGER
    }

    public enum Gender {
        MALE,
        FEMALE,
        THIRD
    }

    @Data
    @EqualsAndHashCode(of = "id")
    @NoArgsConstructor
    @AllArgsConstructor
    @Wither
    public static class Team implements HasId {
        public int id;
        public String name;

        public Team changeName() {
            return withName(faker.team().name());
        }
    }
}
