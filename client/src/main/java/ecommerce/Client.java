package ecommerce;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Client {

    public static final String tag = "[Client]";
    public static final int port = 50071;
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", port).usePlaintext().build();

        var stub1 = ProductInfoGrpc.newBlockingStub(channel);

        // unary add product
        ProductID productID = addProduct(stub1);

        // unary get product
        System.out.printf("%s [R] %s\n", tag, stub1.getProduct(productID));

        var stub2 = OrderManagementGrpc.newBlockingStub(channel);
        var asyncStub = OrderManagementGrpc.newStub(channel);

        // Search Orders, server side streaming
        var matchingOrdersItr = stub2.searchOrders(SearchRequest.newBuilder().setS("Google").build());
        while (matchingOrdersItr.hasNext()) {
            var order = matchingOrdersItr.next();
            logger.info("Matching Order - " + order.getId());
            logger.info(" Order :\n" + order);
        }

        // Update Orders
        updateOrders(asyncStub);

        // Process Order
        processOrders(asyncStub);

        channel.shutdown();

    }

    private static void updateOrders(OrderManagementGrpc.OrderManagementStub asyncStub) {

        Order ord1 = Order.newBuilder()
                .setId("102").addItems("Google Pixel 3A").addItems("Google Pixel Book")
                .setDestination("Mountain View, CA").setPrice(1100).build();
        Order ord2 = Order.newBuilder()
                .setId("103").addItems("Apple Watch S4").addItems("Mac Book Pro").addItems("iPad Pro")
                .setDestination("San Jose, CA").setPrice(2800).build();
        Order ord3 = Order.newBuilder()
                .setId("104").addItems("Google Home Mini").addItems("Google Nest Hub").addItems("iPad Mini")
                .setDestination("Mountain View, CA").setPrice(2200).build();

        final CountDownLatch finishLatch = new CountDownLatch(1);

        var updateOrderResponseObserver = new StreamObserver<OrderIdList>() {
            @Override
            public void onNext(OrderIdList orders) {
                logger.info("Update Orders Res :\n");
                for (var id : orders.getIdsList()) {
                    logger.info(id + ", ");
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                logger.info("Update orders response completed!");
                finishLatch.countDown();
            }
        };

        var updateOrderRequestObserver = asyncStub.updateOrders(updateOrderResponseObserver);
        updateOrderRequestObserver.onNext(ord1);
        updateOrderRequestObserver.onNext(ord2);
        updateOrderRequestObserver.onNext(ord3);
        updateOrderRequestObserver.onNext(ord3);


        if (finishLatch.getCount() == 0) {
            logger.warning("RPC completed or errored before we finished sending.");
            return;
        }
        updateOrderRequestObserver.onCompleted();

        // Receiving happens asynchronously
        try {
            if (!finishLatch.await(10, TimeUnit.SECONDS)) {
                logger.warning("FAILED : Process orders cannot finish within 10 seconds");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static void processOrders(OrderManagementGrpc.OrderManagementStub asyncStub) {

        final CountDownLatch finishLatch = new CountDownLatch(1);


        var orderProcessResponseObserver = new StreamObserver<CombinedShipment>() {
            @Override
            public void onNext(CombinedShipment shipment) {
                logger.info("Combined Shipment : " + shipment.getId() + " : " + shipment.getOrdersListList());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                logger.info("Order Processing completed!");
                finishLatch.countDown();
            }
        };

        var orderProcessRequestObserver =  asyncStub.processOrders(orderProcessResponseObserver);

        orderProcessRequestObserver.onNext(OrderId.newBuilder().setId("102").build());
        orderProcessRequestObserver.onNext(OrderId.newBuilder().setId("103").build());
        orderProcessRequestObserver.onNext(OrderId.newBuilder().setId("104").build());
        orderProcessRequestObserver.onNext(OrderId.newBuilder().setId("101").build());

        if (finishLatch.getCount() == 0) {
            logger.warning("RPC completed or errored before we finished sending.");
            return;
        }
        orderProcessRequestObserver.onCompleted();


        try {
            if (!finishLatch.await(60, TimeUnit.SECONDS)) {
                logger.warning("FAILED : Process orders cannot finish within 60 seconds");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static ProductID addProduct(ProductInfoGrpc.ProductInfoBlockingStub stub) {
        ProductID productID = stub.addProduct(
                Product.newBuilder()
                        .setName("Apple iPhone 14 Pro Max")
                        .setDescription("Pro. Beyond.")
                        .setPrice(1099.0f)
                        .build()
        );

        System.out.printf("%s [C] [SUCCESS] Product ID: %s\n\n", tag, productID.getValue());
        return productID;
    }

}
