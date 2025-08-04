package gorb.vars.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import gorb.vars.model.Message;
import org.apache.kafka.common.serialization.Deserializer;

public class MessageDeserializer implements Deserializer<Message> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Message deserialize(String topic, byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, Message.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error of deserializing message: " + ex.getMessage());
        }

    }
}
