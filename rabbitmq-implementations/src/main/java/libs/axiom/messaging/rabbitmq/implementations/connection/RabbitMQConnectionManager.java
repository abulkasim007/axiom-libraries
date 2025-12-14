package libs.axiom.messaging.rabbitmq.implementations.connection;


import com.rabbitmq.client.BlockedListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.inject.Inject;
import libs.axiom.threading.abstractions.ExecutorServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public class RabbitMQConnectionManager {

    private final ExecutorServiceProvider executorServiceProvider;
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConnectionManager.class);

    @Inject
    public RabbitMQConnectionManager(ExecutorServiceProvider executorServiceProvider) {
        this.executorServiceProvider = executorServiceProvider;
    }

    public Connection createConnection(String connectionString, String connectionName) throws NoSuchAlgorithmException, KeyManagementException {
        if (connectionString == null || connectionString.trim().isEmpty()) {
            throw new IllegalArgumentException("Connection string cannot be null or empty.");
        }

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(new URI(connectionString));
            factory.setSharedExecutor(this.executorServiceProvider.getExecutorService());

            Connection connection = factory.newConnection(connectionName);

            connection.addBlockedListener(new BlockedListener() {
                @Override
                public void handleBlocked(String reason) {
                    logger.info("Connection blocked: {}", reason);
                }

                @Override
                public void handleUnblocked() {
                    logger.info("Connection unblocked");
                }
            });


            if (connection.isOpen()) {
                logger.info("Connection to RabbitMQ: {} established successfully", connection.getAddress());
            }

            return connection;

        } catch (IOException | TimeoutException | java.net.URISyntaxException e) {
            throw new RuntimeException("Failed to create RabbitMQ connection", e);
        }
    }
}