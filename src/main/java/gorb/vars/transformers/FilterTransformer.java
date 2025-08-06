package gorb.vars.transformers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gorb.vars.model.Message;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ДОЛЖНО БЫТЬ:
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.KeyValue;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Фильтрация зацензуренных сообщений.
 */
public class FilterTransformer implements ValueTransformerWithKey<String, String, Message> {
    private ProcessorContext context;
    private KeyValueStore<String, String> blockedStore;
    private KeyValueStore<String, String> censoreStore;
    private final ObjectMapper mapper = new ObjectMapper();

    private Logger logger = LoggerFactory.getLogger(FilterTransformer.class);

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.blockedStore = context.getStateStore("blocked-users-store");
        this.censoreStore = context.getStateStore("censored-words-store");
    }

    @Override
    public Message transform(String key, String value) {
        try {
            Message message = mapper.readValue(value, Message.class);
            logger.info("Фильтрация сообщения {}", message.toString());

            //Проверка, нет ли отправителя в чс.
            try {
                if (isUserBlocked(message)) {
                    return null;
                }
            } catch (RuntimeException ex) {
                return null;
            }

            // Цензура запрещённых слов
            String censoredMessage = censorMessage(censoreStore, message);
            if (censoredMessage != null) {
                logger.info("Текст сообщения успешно зацензурен: {}", censoredMessage);
                message.setText(censoredMessage);
                // Возвращаем пару ключ-значение
                return message;
            } else {
                logger.error("Работа трансформера Оптимуса-Прайма прервана.");
                return null;
            }
        } catch (JsonProcessingException e) {
            logger.error("Ошибка при обработке сообщения: {}", e.getMessage());
            return null; // Пропускаем сообщения с ошибками
        }
    }

    private boolean isUserBlocked(Message message) {
        try {
            // Проверка, заблокан ли юзер-отправитель
            String blockedList = blockedStore.get(message.getTo());
            if (blockedList != null && Arrays.asList(blockedList.split(","))
                    .contains(message.getFrom())) {
                logger.info("Юзер - отправитель найден в списке заблокированных контактов.");
                return true;
            }
            return false;
        } catch (RuntimeException exception) {
            logger.error("Ошибка с парсингом хранилища: {}", exception.getMessage());
            throw new RuntimeException(exception.getMessage());
        }
    }

    private String censorMessage(KeyValueStore<String, String> censoreStore, Message message) {
        String messageText = message.getText();
        try (KeyValueIterator<String, String> iterator = censoreStore.all()) {
            while (iterator.hasNext()) {
                KeyValue<String, String> kvPair = iterator.next();
                String word = kvPair.key;
                if (word == null || word.isEmpty()) continue;
                messageText = messageText.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", "***");
            }
            return messageText;
        } catch (RuntimeException exception) {
            logger.error("Ошибка итерации по хранилищу {}", exception.getMessage());
            return null;
        }
    }

    @Override
    public void close() {

    }
}
