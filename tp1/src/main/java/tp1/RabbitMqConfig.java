package tp1;

import com.rabbitmq.client.ConnectionFactory;

public final class RabbitMqConfig {
    private RabbitMqConfig() {
    }

    public static ConnectionFactory buildFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "localhost"));
        factory.setPort(Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672")));
        factory.setUsername(System.getenv().getOrDefault("RABBITMQ_USER", "student"));
        factory.setPassword(System.getenv().getOrDefault("RABBITMQ_PASS", "student"));
        return factory;
    }
}
