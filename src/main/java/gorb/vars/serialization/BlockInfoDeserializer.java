package gorb.vars.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import gorb.vars.model.BlockInfo;
import org.apache.kafka.common.serialization.Deserializer;

public class BlockInfoDeserializer implements Deserializer<BlockInfo> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public BlockInfo deserialize(String topic, byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, BlockInfo.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error of deserializing message: " + ex.getMessage());
        }
    }
}
