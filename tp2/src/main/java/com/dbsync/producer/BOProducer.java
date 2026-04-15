package com.dbsync.producer;

import com.dbsync.model.SaleRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

// RabbitMQ
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

// SQL
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class BOProducer {

    private static final String RABBITMQ_HOST = "localhost";
    private static final String RABBITMQ_USER = "admin";
    private static final String RABBITMQ_PASS = "admin";

    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    public static void main(String[] args) throws Exception {

        String boName = args.length > 0 ? args[0] : "BO1";
        String dbName = args.length > 1 ? args[1] : "bo1_db";
        String queueName = boName.toLowerCase() + ".sync";
        String dbUrl = "jdbc:mysql://localhost:3308/" + dbName + "?useSSL=false&serverTimezone=UTC";

        System.out.println("=== " + boName + " Producer démarré ===");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername(RABBITMQ_USER);
        factory.setPassword(RABBITMQ_PASS);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        try (com.rabbitmq.client.Connection rabbitConn = factory.newConnection();
             Channel channel = rabbitConn.createChannel()) {

            channel.queueDeclare(queueName, true, false, false, null);

            try (Connection dbConn = DriverManager.getConnection(dbUrl, DB_USER, DB_PASS)) {

                List<SaleRecord> records = fetchUnsyncedRecords(dbConn, boName);

                for (SaleRecord record : records) {
                    String json = mapper.writeValueAsString(record);

                    channel.basicPublish(
                        "",
                        queueName,
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        json.getBytes("UTF-8")
                    );

                    System.out.println("Envoyé : " + record);

                    markAsSynced(dbConn, record.getId());
                }
            }
        }
    }

    private static List<SaleRecord> fetchUnsyncedRecords(Connection conn, String source) throws SQLException {

        List<SaleRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM product_sales WHERE synced = FALSE";

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                SaleRecord r = new SaleRecord();
                r.setId(rs.getInt("id"));
                r.setSource(source);
                r.setSaleDate(rs.getDate("sale_date").toLocalDate());
                r.setRegion(rs.getString("region"));
                r.setProduct(rs.getString("product"));
                r.setQty(rs.getInt("qty"));
                r.setCost(rs.getBigDecimal("cost"));
                r.setAmt(rs.getBigDecimal("amt"));
                r.setTax(rs.getBigDecimal("tax"));
                r.setTotal(rs.getBigDecimal("total"));
                list.add(r);
            }
        }
        return list;
    }

    private static void markAsSynced(Connection conn, int id) throws SQLException {
        String sql = "UPDATE product_sales SET synced = TRUE WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }
}