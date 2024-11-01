import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class SimulatedMicroservices {

    private static final Random RANDOM = new Random();
    private static final int RETRIES = 100;
    private static final ExecutorService PRODUCER_EXECUTOR = Executors.newFixedThreadPool(4);
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");

    private static long basicMessagesProduced = 0;
    private static long transactionalMessagesProduced = 0;
    private static long compactingMessagesProduced = 0;

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("Starting Microservices Simulation");

        createTopics();

        Callable<Boolean> basicProducerTask = () -> {
            Producer<String,String> producer = new KafkaProducer<>(Context.getProducerProperties());
            while (true) {
                ProducerRecord<String,String> record = new ProducerRecord<>(
                        Context.BASIC_TOPIC,
                        UUID.randomUUID().toString());
                producer.send(record);
                basicMessagesProduced++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        };

        Callable<Boolean> transactionalProducerTask = () -> {
            Producer<String,String> producer = new KafkaProducer<>(Context.getTransactionalProducerProperties());
                producer.initTransactions();
                producer.beginTransaction();
                while (true) {

                    ProducerRecord<String, String> record = new ProducerRecord<>(
                            Context.TRANSACTIONAL_TOPIC,
                            UUID.randomUUID().toString());
                    producer.send(record);
                    transactionalMessagesProduced++;
                    // should end the transaction?
                    if (RANDOM.nextBoolean()) {
                        if (RANDOM.nextBoolean()) {
                            producer.commitTransaction();
                        } else {
                            producer.abortTransaction();
                        }
                        producer.beginTransaction();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // ignored
                    }

                }
        };

        Callable<Boolean> compactingProducerTask = () -> {
            Producer<String,String> producer = new KafkaProducer<>(Context.getProducerProperties());
            while (true) {
                ProducerRecord<String,String> record = new ProducerRecord<>(
                        Context.COMPACTING_TOPIC,
                        Context.getRandomKey(),
                        UUID.randomUUID().toString());
                producer.send(record);
                compactingMessagesProduced++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        };

        Callable<Boolean> loggingTask = () -> {
            while(true) {
                List.of(
                        new String[] { Context.BASIC_TOPIC, String.valueOf(basicMessagesProduced) },
                                new String[] { Context.COMPACTING_TOPIC, String.valueOf(compactingMessagesProduced) },
                                new String[] { Context.COMPACTING_TOPIC, String.valueOf(transactionalMessagesProduced) })
                        .forEach(topicMessageCount -> {
                            System.out.println(DATE_FORMAT.format(new Date()) + " --> Produced: " + topicMessageCount[1] + " messages to topic: " + topicMessageCount[0]);
                        });
                Thread.sleep(5000);
            }
        };

        List<Future<Boolean>> futures = PRODUCER_EXECUTOR.invokeAll(
            List.of(basicProducerTask,
                    transactionalProducerTask,
                    compactingProducerTask,
                    loggingTask
            )
        );

        futures.forEach(future -> {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
                // ignored
            }
        });

    }

    private static void createTopics() throws ExecutionException, InterruptedException {

        Admin admin = KafkaAdminClient.create(Context.getKafkaConnectionProperties());
        for (int attempt = 1; attempt < RETRIES +1; attempt++) {

            System.out.println("Creating topics, attempt: " + attempt + " of: " + RETRIES);

            // create topics
            NewTopic basicTopic = new NewTopic(
                    Context.BASIC_TOPIC, 1, (short)1
            );
            NewTopic transactionalTopic = new NewTopic(
                    Context.TRANSACTIONAL_TOPIC, 1, (short)1
            );
            NewTopic compactingTopic = new NewTopic(
                    Context.COMPACTING_TOPIC, 1, (short)1
            );
            compactingTopic.configs(Map.of(
                    "segment.ms", "100",
                    "cleanup.policy", "compact"
            ));

            try {
                admin.createTopics(List.of(
                        basicTopic,
                        transactionalTopic,
                        compactingTopic
                )).all().get();
            } catch (ExecutionException | InterruptedException ex) {
                if (ex.getCause() instanceof TopicExistsException) {
                    // ignored
                } else {
                    throw ex;
                }
            }
            if (admin.listTopics().names().get().containsAll(List.of(
                    "basic",
                    "transactional",
                    "compacting"
            ))) {
                break;
            }
        }
        System.out.println("Created Topics");
    }

}
