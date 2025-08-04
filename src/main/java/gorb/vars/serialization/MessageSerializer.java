package gorb.vars.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import gorb.vars.model.Message;
import org.apache.kafka.common.serialization.Serializer;

public class MessageSerializer implements Serializer<Message> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, Message message) {
        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (Exception ex) {
            throw new RuntimeException("Error of serializing message: " + ex.getMessage());
        }
    }
}
