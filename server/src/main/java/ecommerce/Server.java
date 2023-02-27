package ecommerce;

import io.grpc.ServerBuilder;

import java.io.IOException;

public class Server {

    public static final int port = 50071;
    public static final String tag = "[Server]";
    private static io.grpc.Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        var s = new Server();
        s.start();
        s.blockUntilShutdown();
    }

    private void start() throws IOException {
        /* The port on which the server should run */
        server = ServerBuilder.forPort(port)
                .addService(new ProductInfoImpl())
                .addService(new OrderServiceImpl())
                .build()
                .start();

        System.out.printf("%s [started] listening on: %d\n", tag, port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.printf("%s [Shutting Down] gRPC server since JVM is shutting down\n", tag);
            Server.this.stop();
            System.err.printf("%s [Down]\n", tag);
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
