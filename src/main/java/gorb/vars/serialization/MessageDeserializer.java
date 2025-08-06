package gorb.vars.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import exceptions.DeserializationException;
import gorb.vars.model.Message;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDeserializer implements Deserializer<Message> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(MessageDeserializer.class);

    @Override
    public Message deserialize(String topic, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            logger.debug("Получено пустое сообщение топика {}", topic);
            return null;
        }
        try {
            Message result = objectMapper.readValue(bytes, Message.class);
            logger.info("Успешная десериализация сообщения: topic: {}, message: {}",
                    topic, result.toString());
            return result;
        } catch (Exception ex) {
            logger.error("Ошибка десериализации: {}", ex.getMessage());
            throw new DeserializationException(ex.getMessage());
        }

    }
}
