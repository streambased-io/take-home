import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

public class Context {

    public static final Random RANDOM = new Random();
    public static final String BASIC_TOPIC = "basic";
    public static final String TRANSACTIONAL_TOPIC = "transactional";
    public static final String COMPACTING_TOPIC = "compacting";

    public static final List<String> KEYS = IntStream.range(0,100)
            .mapToObj(e -> "UUID: " + UUID.randomUUID())
            .toList();

    public static Properties getKafkaConnectionProperties() {

        Properties props = new Properties();
        props.setProperty("bootstrap.servers","kafka1:9092");
        return props;
    }

    public static Properties getProducerProperties() {

        Properties props = getKafkaConnectionProperties();
        props.setProperty("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        props.setProperty("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
        return props;
    }

    public static Properties getTransactionalProducerProperties() {

        Properties props = getProducerProperties();
        props.setProperty("transactional.id","myTransactional");
        props.setProperty("enable.idempotence","true");
        return props;
    }

    public static String getRandomKey() {
        return KEYS.get(RANDOM.nextInt(KEYS.size()));
    }

}
