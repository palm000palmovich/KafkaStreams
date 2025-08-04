package gorb.vars.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import gorb.vars.model.BlockInfo;
import org.apache.kafka.common.serialization.Serializer;

public class BlockInfoSerialization implements Serializer<BlockInfo> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String s, BlockInfo blockInfo) {
        try {
            return objectMapper.writeValueAsBytes(blockInfo);
        } catch (Exception ex) {
            throw new RuntimeException("Error of serializing message: " + ex.getMessage());
        }
    }
}
