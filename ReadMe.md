1) Создание топиков: 

`docker exec -it kafka-0 kafka-topics.sh \
--create --topic messages --bootstrap-server \
kafka-0:9092 --partitions 3 --replication-factor 2
docker exec -it kafka-0 kafka-topics.sh \
--create --topic filtered_messages --bootstrap-server \ 
kafka-0:9092 --partitions 3 --replication-factor 2
docker exec -it kafka-0 kafka-topics.sh \
--create --topic blocked_users --bootstrap-server \
kafka-0:9092 --partitions 3 --replication-factor 2
docker exec -it kafka-0 kafka-topics.sh \
--create --topic censored_words --bootstrap-server \
kafka-0:9092 --partitions 3 --replication-factor 2`

2) Отправка сообщения в топик message в формате JSON с сериализацией:

`docker exec -it kafka-0 kafka-console-producer.sh \
  --topic messages \
  --bootstrap-server kafka-0:9092 \
  --property parse.key=true \
  --property key.separator=: \
  --property value.serializer=org.apache.kafka.common.serialization.StringSerializer`

!затем вводим:
`key1:{"from": "user1", "to": "user2", "text": "Hello world"}`

!набираем комбинацию ctrl+D для выхода из режима ввода.


3) Чтобы посмотреть содержимое топика filtered_messages c десериализацией:

`docker exec -it kafka-0 kafka-console-consumer.sh \
  --topic filtered_messages \
  --bootstrap-server kafka-0:9092 \
  --from-beginning \
  --property print.key=true \
  --property print.value=true \
  --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
  --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer`

_*Увидим что-то такое:_
`key1	{"from":"user1","to":"user2","text":"Hello world"}`

!набираем комбинацию ctrl+С для выхода из режима просмотра.

4) Добавить новое запрещенное слово:

`echo 'her1:her' | docker exec -i kafka-0 kafka-console-producer.sh \
  --topic censored_words \
  --bootstrap-server kafka-0:9092 \
  --property parse.key=true \
  --property key.separator=:`

_здесь "word1" - ключ, "badword" - значение_

!!! Ключ, как ни странно должен быть всегда уникальным !!!

! Просмотр списка запрещенных слов:

`docker exec -it kafka-0 kafka-console-consumer.sh \
  --topic censored_words \
  --bootstrap-server kafka-0:9092 \
  --from-beginning \
  --property print.key=true \
  --property key.separator=": " \
  --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer`

Вывод:
`her1: her`

**Тестинг фунционала фильтрации**

-отправим сообщение с запрещенным словом:

`echo 'msg1:{"from": "user1", "to": "user2", "text": "Hello world, this her is her a her"}' | \
docker exec -i kafka-0 kafka-console-producer.sh \
  --topic messages \
  --bootstrap-server kafka-0:9092 \
  --property parse.key=true \
  --property key.separator=: \
  --property value.serializer=org.apache.kafka.common.serialization.StringSerializer`

-Проверим топик filtered_messages:

`docker exec -it kafka-0 kafka-console-consumer.sh \
  --topic filtered_messages \
  --bootstrap-server kafka-0:9092 \
  --from-beginning \
  --property print.key=true \
  --property print.value=true \
  --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
  --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer`

Вывод:
`msg1	{"from":"user1","to":"user2","text":"Hello world, this *** is *** a ***"}`

5) Заблочим пользователя:

`echo 'block1:{"blockingUser": "user2", "blockedUser": "user1"}' | \
docker exec -i kafka-0 kafka-console-producer.sh \
  --topic blocked_users \
  --bootstrap-server kafka-0:9092 \
  --property parse.key=true \
  --property key.separator=: \
  --property value.serializer=org.apache.kafka.common.serialization.StringSerializer`

-Топик blocked_users:

`docker exec -it kafka-0 kafka-console-consumer.sh \
  --topic blocked_users \
  --bootstrap-server kafka-0:9092 \
  --from-beginning \
  --property print.key=true \
  --property key.separator=": " \
  --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer`

Вывод:
`block1: {"blockingUser": "user2", "blockedUser": "user1"}`

***Тестинг функционала с блокировкой юзера.***

-Отправим сообщение от заблокированного юзера заблокировавшему его юзеру: 

`echo 'msg2:{"from": "user1", "to": "user2", "text": "This should be blocked"}' | \
docker exec -i kafka-0 kafka-console-producer.sh \
  --topic messages \
  --bootstrap-server kafka-0:9092 \
  --property parse.key=true \
  --property key.separator=: \
  --property value.serializer=org.apache.kafka.common.serialization.StringSerializer`


-Проверим топик filtered_message, что сообщение от user1 не дошло до user2:

`docker exec -it kafka-0 kafka-console-consumer.sh \
  --topic filtered_messages \
  --bootstrap-server kafka-0:9092 \
  --from-beginning \
  --property print.key=true \
  --property print.value=true \
  --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
  --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer`

Вывод:
*Тут пусто*

-Отправим сообщение от незаблокированного пользователя:

`echo 'msg3:{"from": "user3", "to": "user2", "text": "This should pass"}' | \
docker exec -i kafka-0 kafka-console-producer.sh \
  --topic messages \
  --bootstrap-server kafka-0:9092 \
  --property parse.key=true \
  --property key.separator=: \
  --property value.serializer=org.apache.kafka.common.serialization.StringSerializer`

Содержимое топика filtered_message:
`msg3	{"from":"user3","to":"user2","text":"This should pass"}`