package gorb.vars.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import gorb.vars.model.Message;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageSerializer implements Serializer<Message> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(MessageSerializer.class);

    @Override
    public byte[] serialize(String topic, Message message) {
        if (message == null) {
            logger.debug("Получено пустое сообщение топика {}", topic);
            return null;
        }

        try {
            byte[] result = objectMapper.writeValueAsBytes(message);
            logger.info("Успешная сериализация сообщения: topic: {}, message: {}",
                    topic, message.toString());
            return result;
        } catch (Exception ex) {
            String errorMessage = String
                    .format("Ошибка сериализации сообщения топика %s: %s",
                            topic, message);
            logger.error(errorMessage, ex.getMessage());
            throw new SerializationException(errorMessage, ex);
        }
    }
}
