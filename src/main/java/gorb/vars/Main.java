package gorb.vars;

import gorb.vars.model.Message;
import gorb.vars.processors.BlockProcessor;
import gorb.vars.processors.CensorProcessor;
import gorb.vars.serialization.MessageDeserializer;
import gorb.vars.serialization.MessageSerializer;
import gorb.vars.transformers.FilterTransformer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "message-filter-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);

        StreamsBuilder builder = new StreamsBuilder();

        // State Store: заблокированные пользователи
        StoreBuilder<KeyValueStore<String, String>> blockedStore = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("blocked-users-store"),
                Serdes.String(),
                Serdes.String()
        );
        builder.addStateStore(blockedStore);

        // State Store: запрещённые слова
        StoreBuilder<KeyValueStore<String, String>> censorStore = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("censored-words-store"),
                Serdes.String(),
                Serdes.String()
        );
        builder.addStateStore(censorStore);

        // Поток: сообщения о блокнутых юзерах
        KStream<String, String> blockUsersStream = builder.stream("blocked_users");
        //Топик для добавления блокнутых юзеров в потоковом режиме
        blockUsersStream.process(BlockProcessor::new, "blocked-users-store");

        // Поток: сообщения о запрещённых слов
        KStream<String, String> censoredWords = builder.stream("censored_words");
        //Топик для добавления запрещенных слов в потоковом режиме
        censoredWords.process(CensorProcessor::new, "censored-words-store");

        // Поток: входящие сообщения
        KStream<String, String> messagesStream = builder.stream("messages");
        //Фильтрация и преобразование сообщений
        KStream<String, Message> filteredMessages = messagesStream
                .transformValues(FilterTransformer::new,
                        "blocked-users-store", "censored-words-store")
                .filter((key, msg) -> msg != null); // убрать заблокированные

        // Запись в выходной топик
        final Serde<Message> messageSerde = Serdes.serdeFrom(new MessageSerializer(), new MessageDeserializer());
        filteredMessages.to("filtered_messages", Produced.with(Serdes.String(), messageSerde));

        // Сборка и запуск
        Topology topology = builder.build();
        System.out.println("Полученная топология: " + topology.describe());;

        KafkaStreams streams = new KafkaStreams(topology, props);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        streams.start();

        // Добавьте ожидание завершения
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            streams.close();
            latch.countDown();
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            System.exit(1);
        }
        System.exit(0);
    }
}