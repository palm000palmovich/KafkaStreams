package exceptions;

import org.apache.kafka.common.errors.SerializationException;

public class DeserializationException extends SerializationException {
    public DeserializationException(String message) {
      super(message);
    }
}
