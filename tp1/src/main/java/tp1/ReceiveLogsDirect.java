package tp1;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class ReceiveLogsDirect {
    private static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = RabbitMqConfig.buildFactory();
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        String queueName = channel.queueDeclare().getQueue();

        List<String> severities = argv.length == 0 ? Arrays.asList("info", "warning", "error") : Arrays.asList(argv);
        for (String severity : severities) {
            channel.queueBind(queueName, EXCHANGE_NAME, severity);
        }

        System.out.println(" [*] Waiting for logs " + severities + ". Press CTRL+C to exit");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String routingKey = delivery.getEnvelope().getRoutingKey();
            System.out.println(" [x] Received '" + routingKey + ":" + message + "'");
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                channel.close();
                connection.close();
            } catch (Exception ignored) {
            }
        }));
    }
}
