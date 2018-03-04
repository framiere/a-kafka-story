# Objective

Joins

# Docker

```
$ docker-compose up -d
$ docker-compose logs -f producer
producer_1      | CHANGE_COUNTRY Domain.Address(id=38, streetName=Serenity Points, streetAddress=024 Macejkovic Knoll, city=Townestad, state=New Jersey, country=Bulgaria)
producer_1      | TEAM_NAME_CHANGE Domain.Team(id=9, name=Pennsylvania vampires)
producer_1      | NEW_MEMBER Domain.Member(id=78, firstname=Percy, lastname=Glover, gender=MALE, maritalStatus=MARRIED, teamId=24, age=45, role=QA)
producer_1      | NEW_MEMBER Domain.Address(id=78, streetName=Cicero Loaf, streetAddress=37934 Mayer Wall, city=Port Vito, state=Iowa, country=Bosnia and Herzegovina)
producer_1      | CHANGE_COUNTRY Domain.Address(id=42, streetName=Wilbert Fields, streetAddress=33501 Veronica Canyon, city=North Lincolnmouth, state=Connecticut, country=Puerto Rico)
producer_1      | CHANGE_ROLE Domain.Member(id=68, firstname=Adella, lastname=Hessel, gender=MALE, maritalStatus=DIVORCED, teamId=15, age=10, role=DEVELOPER)
producer_1      | NEW_MEMBER Domain.Member(id=80, firstname=Hazle, lastname=Herzog, gender=FEMALE, maritalStatus=SINGLE, teamId=24, age=22, role=QA)
producer_1      | NEW_MEMBER Domain.Address(id=80, streetName=Hartmann Shoals, streetAddress=7054 Ratke Curve, city=New Theresa, state=Colorado, country=Sudan)
producer_1      | TEAM_NAME_CHANGE Domain.Team(id=10, name=Nebraska zebras)
producer_1      | NEW_MEMBER Domain.Member(id=82, firstname=Ernest, lastname=Legros, gender=THIRD, maritalStatus=WIDOWED, teamId=19, age=5, role=MANAGER)
producer_1      | NEW_MEMBER Domain.Address(id=82, streetName=Zander Creek, streetAddress=721 Spencer Lakes, city=Mannshire, state=Iowa, country=Netherlands)
$ docker-compose exec kafka-1 kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic Team \
    --property print.key=true \
    --key-deserializer org.apache.kafka.common.serialization.IntegerDeserializer \
    --from-beginning
1	{"id":1,"name":"Michigan gooses"}
6	{"id":6,"name":"Tennessee banshees"}
3	null
1	{"id":1,"name":"Delaware worshipers"}
10	{"id":10,"name":"Connecticut conspirators"}
6	{"id":6,"name":"New Jersey rabbits"}
10	{"id":10,"name":"Florida oracles"}
7	null
10	{"id":10,"name":"Louisiana sons"}
1	null
docker-compose exec kafka-1 kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic Aggregate \
    --property print.key=true \
    --key-deserializer org.apache.kafka.common.serialization.IntegerDeserializer \
    --from-beginning
$ docker-compose exec kafka-1 kafka-topics \
    --zookeeper zookeeper:2181 \
    --list
Address
Aggregate
Member
Team
__consumer_offsets
simple-join-stream-KSTREAM-JOINOTHER-0000000009-store-changelog
simple-join-stream-KSTREAM-JOINTHIS-0000000008-store-changelog
simple-join-stream-KSTREAM-OUTEROTHER-0000000014-store-changelog
simple-join-stream-KSTREAM-OUTERTHIS-0000000013-store-changelog
$ docker-compose exec kafka-1 kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic Aggregate \
    --property print.key=true \
    --key-deserializer org.apache.kafka.common.serialization.IntegerDeserializer \
    --from-beginning
5	{"member":{"id":5,"firstname":"Katelin","lastname":"Donnelly","gender":"FEMALE","maritalStatus":"SINGLE","teamId":8,"age":22,"role":"MANAGER"},"address":{"id":5,"streetName":"Upton Square","streetAddress":"97265 Yost Springs","city":"West Majorton","state":"Rhode Island","country":"USA"},"team":{"id":5,"name":"Missouri dwarves"}}
5	{"member":{"id":5,"firstname":"Katelin","lastname":"Donnelly","gender":"FEMALE","maritalStatus":"SINGLE","teamId":8,"age":22,"role":"MANAGER"},"address":{"id":5,"streetName":"Upton Square","streetAddress":"97265 Yost Springs","city":"West Majorton","state":"Rhode Island","country":"USA"},"team":{"id":5,"name":"Rhode Island goblins"}}
5	{"member":{"id":5,"firstname":"Katelin","lastname":"Donnelly","gender":"FEMALE","maritalStatus":"SINGLE","teamId":8,"age":22,"role":"MANAGER"},"address":{"id":5,"streetName":"Upton Square","streetAddress":"97265 Yost Springs","city":"West Majorton","state":"Rhode Island","country":"USA"},"team":{"id":5,"name":"Hawaii spirits"}}
20	{"member":null,"address":null,"team":{"id":20,"name":"Louisiana ghosts"}}
$ docker-compose logs -f joinstreamer
joinstreamer_1  | 5:Domain.Aggregate(member=Domain.Member(id=5, firstname=Katelin, lastname=Von, gender=FEMALE, maritalStatus=DIVORCED, teamId=8, age=21, role=MANAGER), address=Domain.Address(id=5, streetName=Crawford Street, streetAddress=34122 Claudie Squares, city=South Jenatown, state=Rhode Island, country=USA), team=Domain.Team(id=5, name=New Jersey horses))
joinstreamer_1  | 5:Domain.Aggregate(member=Domain.Member(id=5, firstname=Katelin, lastname=Donnelly, gender=FEMALE, maritalStatus=SINGLE, teamId=8, age=22, role=MANAGER), address=Domain.Address(id=5, streetName=Myrna Knolls, streetAddress=45080 Lessie Crest, city=South Jenatown, state=Rhode Island, country=USA), team=Domain.Team(id=5, name=New Jersey horses))
joinstreamer_1  | 5:Domain.Aggregate(member=Domain.Member(id=5, firstname=Katelin, lastname=Donnelly, gender=FEMALE, maritalStatus=SINGLE, teamId=8, age=22, role=MANAGER), address=Domain.Address(id=5, streetName=Crawford Street, streetAddress=34122 Claudie Squares, city=South Jenatown, state=Rhode Island, country=USA), team=Domain.Team(id=5, name=New Jersey horses))
joinstreamer_1  | 5:Domain.Aggregate(member=Domain.Member(id=5, firstname=Katelin, lastname=Donnelly, gender=FEMALE, maritalStatus=SINGLE, teamId=8, age=21, role=MANAGER), address=Domain.Address(id=5, streetName=Upton Square, streetAddress=97265 Yost Springs, city=West Majorton, state=Rhode Island, country=USA), team=Domain.Team(id=5, name=New Jersey horses))
joinstreamer_1  | 5:Domain.Aggregate(member=Domain.Member(id=5, firstname=Rebeka, lastname=Donnelly, gender=THIRD, maritalStatus=SINGLE, teamId=8, age=21, role=MANAGER), address=Domain.Address(id=5, streetName=Upton Square, streetAddress=97265 Yost Springs, city=West Majorton, state=Rhode Island, country=USA), team=Domain.Team(id=5, name=New Jersey horses))
joinstreamer_1  | 5:Domain.Aggregate(member=Domain.Member(id=5, firstname=Katelin, lastname=Von, gender=FEMALE, maritalStatus=DIVORCED, teamId=8, age=21, role=MANAGER), address=Domain.Address(id=5, streetName=Upton Square, streetAddress
```
