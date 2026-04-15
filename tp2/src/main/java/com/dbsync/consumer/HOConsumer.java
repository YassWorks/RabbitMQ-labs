package com.dbsync.consumer;

import com.dbsync.model.SaleRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

// SQL
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

// RabbitMQ
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

public class HOConsumer {

    private static final String RABBITMQ_HOST = "localhost";
    private static final String RABBITMQ_USER = "admin";
    private static final String RABBITMQ_PASS = "admin";

    private static final String HO_DB_URL = "jdbc:mysql://localhost:3308/ho_db?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private static final String[] QUEUES = {"bo1.sync", "bo2.sync"};

    public static void main(String[] args) throws Exception {

        System.out.println("=== HO Consumer démarré ===");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername(RABBITMQ_USER);
        factory.setPassword(RABBITMQ_PASS);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Connection dbConn = DriverManager.getConnection(HO_DB_URL, DB_USER, DB_PASS);
        dbConn.setAutoCommit(false);

        com.rabbitmq.client.Connection rabbitConn = factory.newConnection();
        Channel channel = rabbitConn.createChannel();

        for (String queue : QUEUES) {
            channel.queueDeclare(queue, true, false, false, null);
        }

        channel.basicQos(1);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String json = new String(delivery.getBody(), StandardCharsets.UTF_8);

            try {
                SaleRecord record = mapper.readValue(json, SaleRecord.class);

                insertIntoHO(dbConn, record);
                dbConn.commit();

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                System.out.println("✓ Insert OK: " + record);

            } catch (Exception e) {
                try {
                    dbConn.rollback();
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        for (String queue : QUEUES) {
            channel.basicConsume(queue, false, deliverCallback, consumerTag -> {});
        }
    }

    private static void insertIntoHO(Connection conn, SaleRecord r) throws SQLException {

        String checkSql = "SELECT COUNT(*) FROM product_sales WHERE source = ? AND original_id = ?";
        try (PreparedStatement checkPst = conn.prepareStatement(checkSql)) {
            checkPst.setString(1, r.getSource());
            checkPst.setInt(2, r.getId());

            ResultSet rs = checkPst.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Doublon ignoré: " + r.getId());
                return;
            }
        }

        String sql = """
            INSERT INTO product_sales
            (source, original_id, sale_date, region, product, qty, cost, amt, tax, total)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, r.getSource());
            pst.setInt(2, r.getId());
            pst.setDate(3, Date.valueOf(r.getSaleDate()));
            pst.setString(4, r.getRegion());
            pst.setString(5, r.getProduct());
            pst.setInt(6, r.getQty());
            pst.setBigDecimal(7, r.getCost());
            pst.setBigDecimal(8, r.getAmt());
            pst.setBigDecimal(9, r.getTax());
            pst.setBigDecimal(10, r.getTotal());
            pst.executeUpdate();
        }
    }
}