package com.tudux.demos.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoGracefulShutDown {

    private static final Logger log = LoggerFactory.getLogger(ConsumerDemoGracefulShutDown.class.getSimpleName());

    public static void main(String[] args) {

        log.info("Starting Kafka Consumer");

        String boostrapServer = "127.0.0.1:9092";
        String groupId = "grp-con-2";
        String topic = "demo_java";

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,boostrapServer);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String,String> consumer = new KafkaConsumer<String, String>(properties);

        //get reference to current thread
        final Thread mainThread = Thread.currentThread();

        //adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("Detected a shutdown , lets exit by calling consumer.wakeup()...");
                consumer.wakeup();

                //join the main thread to allow execution of the code in the main thread
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        try {

            consumer.subscribe(Arrays.asList(topic));

            while(true){
                log.info("Polling");

                ConsumerRecords<String,String> records =
                        consumer.poll(Duration.ofMillis(1000));

                for(ConsumerRecord<String,String> record : records) {
                    log.info("Key: " + record.key() + " Value: " + record.value());
                    log.info("Partition: " + record.partition() + " Offset: " + record.offset());
                }
            }

        } catch (WakeupException e) {
            //this one is ignored as is an expected exception after closing a consumer
            log.info("Wake up exception!");
        } catch (Exception e) {
            log.error("Unexpected exception");
        } finally {
            consumer.close();
            log.info("The consumer is now gracefully closed");
        }

    }
}
