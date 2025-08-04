package gorb.vars.processors;

import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Добавляет запрещенные слова.
 */
public class CensorProcessor implements Processor<String, String> {
    private ProcessorContext context;
    /**
     * Хранилище запрещенных слов.
     */
    private KeyValueStore<String, String> censoreStore;

    private final Logger logger = LoggerFactory.getLogger(CensorProcessor.class);

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.censoreStore = context
                .getStateStore("censored-words-store");
    }

    @Override
    public void process(String key, String word) {
        if (word != null && !word.trim().isEmpty()) {
            censoreStore.put(word.trim().toLowerCase(), "REDACTED");
            logger.info("Добавлено новое запрещенное слово: " + word.trim().toLowerCase());
        }
    }

    @Override
    public void close() {}
}
