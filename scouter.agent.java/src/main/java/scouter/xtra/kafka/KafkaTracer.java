package scouter.xtra.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import scouter.agent.Logger;
import scouter.agent.proxy.IKafkaTracer;

import java.util.List;

public class KafkaTracer implements IKafkaTracer {
    public String getBootstrapServer(Object kafkaConfig) {
        try {
            if (kafkaConfig instanceof ProducerConfig) {
                ProducerConfig producerConfig = (ProducerConfig) kafkaConfig;
                // BOOTSTRAP_SERVERS_CONFIG is List Type (ref. ProducerConfig)
                List<String> bootstrap = producerConfig.getList(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
                return bootstrap.toString();
            }
        } catch (Throwable th) {
            Logger.println("XTRA-KAFKA-PRPDUCER", th.getMessage(), th);
        }
        return "unknown-config";
    }
}
