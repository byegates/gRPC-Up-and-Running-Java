package ecommerce;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Client {

    public static final String tag = "[Client]";
    public static final int port = 50071;

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", port).usePlaintext().build();

        var stub1 = ProductInfoGrpc.newBlockingStub(channel);

        // unary add product
        ProductID productID = addProduct(stub1);

        // unary get product
        getProduct(stub1, productID);

        var stub2 = OrderManagementGrpc.newBlockingStub(channel);
        var asyncStub = OrderManagementGrpc.newStub(channel);

        // Search Orders, server side streaming
        searchOrders(stub2);

        // Update Orders, client side streaming
        updateOrders(asyncStub);

        // Process Order, bidirectional streaming
        processOrders(asyncStub);

        channel.shutdown();

    }

    private static void getProduct(ProductInfoGrpc.ProductInfoBlockingStub stub, ProductID id) {
        var tag0 = tag + " [R]";
        System.out.printf("%s [Invoked]\n", tag0);
        System.out.printf("%s \n%s", tag0, stub.getProduct(id));
        System.out.printf("%s [END]\n\n", tag0);
    }

    private static void searchOrders(OrderManagementGrpc.OrderManagementBlockingStub stub2) {
        var tag0 = tag + " [SS]";
        System.out.printf("%s [Invoked]\n", tag0);
        var matchingOrdersItr = stub2.searchOrders(SearchRequest.newBuilder().setS("Google").build());
        while (matchingOrdersItr.hasNext()) {
            var order = matchingOrdersItr.next();
            System.out.printf("%s Order :\n%s", tag0, order);
        }
        System.out.printf("%s [END]\n\n", tag0);
    }

    private static void updateOrders(OrderManagementGrpc.OrderManagementStub asyncStub) {
        var tag0 = tag + " [CS]";
        System.out.printf("%s [Invoked]\n", tag0);

        List<Order> orders = List.of(
                Order.newBuilder().setId("102").setDestination("Mountain View, CA").setPrice(1100)
                        .addItems("Google Pixel 3A").addItems("Google Pixel Book").build(),
                Order.newBuilder().setId("103").setDestination("San Jose, CA").setPrice(2800)
                        .addItems("Apple Watch S4").addItems("Mac Book Pro").addItems("iPad Pro").build(),
                Order.newBuilder().setId("104").setDestination("Mountain View, CA").setPrice(2200)
                        .addItems("Google Home Mini").addItems("Google Nest Hub").addItems("iPad Mini").build()
        );

        final CountDownLatch latch = new CountDownLatch(1);

        var responseObserver = new StreamObserver<OrderIdList>() {
            @Override
            public void onNext(OrderIdList orders) {
                System.out.printf("%s Update Orders :\n", tag0);
                for (var id : orders.getIdsList()) {
                    System.out.printf("%s, ", id);
                }
                System.out.println();
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {
                System.out.printf("%s Update orders response completed!\n", tag0);
                latch.countDown();
            }
        };

        var requestObserver = asyncStub.updateOrders(responseObserver);
        for (var ord : orders) {
            requestObserver.onNext(ord);
        }
        requestObserver.onNext(orders.get(orders.size()-1));

        if (latch.getCount() == 0) {
            System.out.printf("%s RPC completed or errored before we finished sending.", tag0);
            return;
        }
        requestObserver.onCompleted();

        // Receiving happens asynchronously
        try {
            int timeout = 10;
            if (!latch.await(timeout, TimeUnit.SECONDS)) {
                System.out.printf("%s [FAILED] Process orders cannot finish within %d seconds.\n", tag0, timeout);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.printf("%s [END]\n\n", tag0);
    }

    private static void processOrders(OrderManagementGrpc.OrderManagementStub asyncStub) {
        var tag0 = tag + " [BI]";
        System.out.printf("%s [Invoked]\n", tag0);

        final CountDownLatch latch = new CountDownLatch(1);

        var responseObserver = new StreamObserver<CombinedShipment>() {
            @Override
            public void onNext(CombinedShipment shipment) {
                System.out.printf("%s Combined Shipment :\n%s :\n%s\n", tag0, shipment.getId(), shipment.getOrdersListList());
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {
                System.out.printf("%s Order Processing completed!\n", tag0);
                latch.countDown();
            }
        };

        var requestObserver =  asyncStub.processOrders(responseObserver);

        List<String> ids = List.of("102", "103", "104", "101");
        for (var id : ids) {
            requestObserver.onNext(OrderId.newBuilder().setId(id).build());
        }

        if (latch.getCount() == 0) {
            System.out.printf("%s RPC completed or errored before we finished sending.\n", tag0);
            return;
        }
        requestObserver.onCompleted();

        try {
            int timeout = 60;
            if (!latch.await(timeout, TimeUnit.SECONDS)) {
                System.out.printf("%s [FAILED] Process orders cannot finish within %d seconds.\n", tag0, timeout);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.printf("%s [END]\n\n", tag0);
    }


    private static ProductID addProduct(ProductInfoGrpc.ProductInfoBlockingStub stub) {
        var tag0 = tag + " [C]";
        System.out.printf("%s [Invoked]\n", tag0);
        ProductID productID = stub.addProduct(
                Product.newBuilder()
                        .setName("Apple iPhone 14 Pro Max")
                        .setDescription("Pro. Beyond.")
                        .setPrice(1099.0f)
                        .build()
        );

        System.out.printf("%s [SUCCESS] Product ID: %s\n", tag0, productID.getValue());
        System.out.printf("%s [END]\n\n", tag0);
        return productID;
    }

}
