package ecommerce;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ProductInfoClient {

    public static final String tag = "[Client]";
    public static final int port = 50071;

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", port)
                .usePlaintext()
                .build();

        ProductInfoGrpc.ProductInfoBlockingStub stub = ProductInfoGrpc.newBlockingStub(channel);

        ProductID productID = stub.addProduct(
                Product.newBuilder()
                        .setName("Apple iPhone 14 Pro Max")
                        .setDescription("Pro. Beyond.")
                        .setPrice(1099.0f)
                        .build()
        );

        System.out.printf("%s [C] [SUCCESS] Product ID: %s\n\n", tag, productID.getValue());

        var product = stub.getProduct(productID);

        System.out.printf("%s [R] %s\n\n", tag, product);
        channel.shutdown();
    }
}
