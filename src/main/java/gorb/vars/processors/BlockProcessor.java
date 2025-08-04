package gorb.vars.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import gorb.vars.model.BlockInfo;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Добавляет информацию по блокировкам пользователей.
 */
public class BlockProcessor implements Processor<String, String> {
    private ProcessorContext context;
    /**
     * Хранилище заблокированных юзеров.
     */
    private KeyValueStore<String, String> blockedStore;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Logger logger = LoggerFactory.getLogger(BlockProcessor.class);

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.blockedStore = context
                .getStateStore("blocked-users-store");
    }

    @Override
    public void process(String key, String value) {
        try {
            BlockInfo event = mapper.readValue(value, BlockInfo.class);
            String owner = event.getBlockingUser();
            String blocked = event.getBlockedUser();

            /**
             * Текущий ЧС.
             */
            String current = blockedStore.get(owner);
            Set<String> blockedSet = current == null ? new HashSet<>()
                    : new HashSet<>(Arrays.asList(current.split(",")));

            /**
             * Обновляем ЧС.
             */
            blockedSet.add(blocked);
            blockedStore.put(owner, String.join(",", blockedSet)); //Обновляем StateStore

            logger.info("User {} blocked {}", owner, blocked);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void close() {}
}
