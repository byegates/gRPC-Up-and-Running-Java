package ecommerce;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class ProductInfoServer {

    public static final int port = 50071;
    public static final String tag = "[Server]";

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(port)
                .addService(new ProductInfoImpl())
                .build()
                .start();

        System.out.printf("%s [started] listening on: %d\n", tag, port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.printf("%s [Shutting Down] gRPC server since JVM is shutting down\n", tag);
            if (server != null) {
                server.shutdown();
            }

            System.err.printf("%s [Down]\n", tag);
        }));

        server.awaitTermination();
    }
}
