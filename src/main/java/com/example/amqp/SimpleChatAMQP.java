package com.example.amqp;

import com.rabbitmq.client.*;
import lombok.SneakyThrows;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;


@SpringBootApplication
public class SimpleChatAMQP implements CommandLineRunner {

    @SneakyThrows
    public static void main(String[] args) {
        SpringApplication.run(SimpleChatAMQP.class, args);

    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println("Oda ismi giriniz... ");
        String chatRoomName = new Scanner(System.in).nextLine();
        System.out.println("Kullanıcı ismi giriniz... ");
        String userName = new Scanner(System.in).nextLine();
        System.out.println("Şifre giriniz... ");
        String password = new Scanner(System.in).nextLine();
        String queueName = "";
        String exchangeName = "chat2";

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setUsername(userName);
        connectionFactory.setPassword(password);
        connectionFactory.setPort(5672);

        Connection conn = connectionFactory.newConnection();
        Channel channel = conn.createChannel();


        channel.exchangeDeclare(exchangeName, "direct", true);
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, exchangeName, chatRoomName);


        boolean autoAck = false;
        channel.basicConsume(queueName, autoAck, "a-consumer-tag",
                new DefaultConsumer(channel) {
                    @SneakyThrows
                    @Override
                    public void handleDelivery(String consumerTag,
                                               Envelope envelope,
                                               AMQP.BasicProperties properties,
                                               byte[] body) {
                        //long deliveryTag = envelope.getDeliveryTag();
                        // requeue the delivery
                        String user = properties.getUserId();
                        String s = new String(body, "UTF-8");

                        System.out.println(user + ": " + s);

                    }
                });
        String input = new Scanner(System.in).nextLine();
        while (!input.isEmpty()) {

            AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
            propsBuilder.userId(userName);
            AMQP.BasicProperties props = propsBuilder.build();

            byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
            channel.basicPublish(exchangeName, chatRoomName, props, bytes);
            Scanner scanner = new Scanner(System.in);

            input = scanner.nextLine();
        }

        channel.close();
        conn.close();
    }
}

