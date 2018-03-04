package com.github.framiere;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import static com.github.framiere.Domain.OperationMode.*;

public class Domain {

    @Data
    @Entity
    @Table(name = "Member")
    @EqualsAndHashCode(of = "id")
    @NoArgsConstructor
    @AllArgsConstructor
    @Wither
    public static class Member {
        @Id
        public int id;
        public String firstname;
        public String lastname;
        public Gender gender;
        public String phone;
        public MaritalStatus maritalStatus;
        public int teamId;
        public int age;
        public Role role;
    }

    @Data
    @Entity
    @Table(name = "Address")
    @EqualsAndHashCode(of = "id")
    @NoArgsConstructor
    @AllArgsConstructor
    @Wither
    public static class Address {
        @Id
        public int id;
        public String streetName;
        public String streetAddress;
        public String city;
        public String state;
        public String country;
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
    @Entity
    @Table(name = "Team")
    @EqualsAndHashCode(of = "id")
    @NoArgsConstructor
    @AllArgsConstructor
    @Wither
    public static class Team {
        @Id
        public int id;
        public String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Wither
    @EqualsAndHashCode(of = "id")
    public static class Aggregate {
        public Member member;
        public Address address;
        public Team team;
    }

    @AllArgsConstructor
    enum Operation {
        NEW_TEAM(8, INSERT),
        NEW_MEMBER(15, INSERT),
        TEAM_NAME_CHANGE(20, UPDATE),
        DELETE_TEAM(3, DELETE),
        DELETE_MEMBER(4, DELETE),
        NEW_MARITAL_STATUS(5, UPDATE),
        CHANGE_PHONE(2, UPDATE),
        CHANGE_ADDRESS_IN_TOWN(5, UPDATE),
        CHANGE_CITY(4, UPDATE),
        CHANGE_COUNTRY(1, UPDATE),
        CHANGE_GENDER(1, UPDATE),
        CHANGE_TEAM(5, UPDATE),
        CHANGE_ROLE(11, UPDATE),
        ANNIVERSARY(2, UPDATE),
        NO_OP(100, NONE);
        int chanceOfHappening;
        OperationMode operationMode;
    }

    enum OperationMode {
        INSERT,
        DELETE,
        UPDATE,
        NONE
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RandomProducerAction {
        public Operation operation;
        public String clazz;
        public Object object;
    }
}
