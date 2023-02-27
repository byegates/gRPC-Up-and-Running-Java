package ecommerce;

import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ecommerce.Server.tag;

public class OrderServiceImpl extends OrderManagementGrpc.OrderManagementImplBase {
    private final Order ord1 = Order.newBuilder()
            .setId("102").addItems("Google Pixel 3A").addItems("Mac Book Pro")
            .setDestination("Mountain View, CA").setPrice(1800).build();
    private final Order ord2 = Order.newBuilder()
            .setId("103").addItems("Apple Watch S4")
            .setDestination("San Jose, CA").setPrice(400).build();
    private final Order ord3 = Order.newBuilder()
            .setId("104").addItems("Google Home Mini").addItems("Google Nest Hub")
            .setDestination("Mountain View, CA").setPrice(400).build();
    private final Order ord4 = Order.newBuilder()
            .setId("105").addItems("Amazon Echo")
            .setDestination("San Jose, CA").setPrice(30).build();
    private final Order ord5 = Order.newBuilder()
            .setId("106").addItems("Amazon Echo").addItems("Apple iPhone XS")
            .setDestination("Mountain View, CA").setPrice(300).build();

    private final Map<String, Order> orderMap = Stream.of(
                    new AbstractMap.SimpleEntry<>(ord1.getId(), ord1),
                    new AbstractMap.SimpleEntry<>(ord2.getId(), ord2),
                    new AbstractMap.SimpleEntry<>(ord3.getId(), ord3),
                    new AbstractMap.SimpleEntry<>(ord4.getId(), ord4),
                    new AbstractMap.SimpleEntry<>(ord5.getId(), ord5))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private final Map<String, CombinedShipment> combinedShipmentMap = new HashMap<>();

    public static final int BATCH_SIZE = 3;

    // Unary
    @Override
    public void addOrder(Order request, StreamObserver<OrderId> responseObserver) {
        var tag0 = tag + " [C]";
        System.out.printf("%s [Invoked]\n", tag0);
        System.out.printf("%s Order Added - ID: %s\n, Destination :\n%s\n", tag0, request.getId(), request.getDestination());
        orderMap.put(request.getId(), request);
        responseObserver.onNext(OrderId.newBuilder().setId("100500").build());
        responseObserver.onCompleted();
        System.out.printf("%s [END]\n\n", tag0);
    }

    // Unary
    @Override
    public void getOrder(OrderId request, StreamObserver<Order> responseObserver) {
        var tag0 = tag + " [R]";
        System.out.printf("%s [Invoked]\n", tag0);
        Order order = orderMap.get(request.getId());
        if (order != null) {
            System.out.printf("%s Order Retrieved : ID - %s\n", tag0, order.getId());
            responseObserver.onNext(order);
            responseObserver.onCompleted();
        } else  {
            System.out.printf("%s [Not Found] %s\n", tag0, request.getId());
            responseObserver.onCompleted();
        }
        // ToDo  Handle errors
        // responseObserver.onError();
        System.out.printf("%s [END]\n\n", tag0);
    }

    // Server Streaming
    @Override
    public void searchOrders(SearchRequest request, StreamObserver<Order> responseObserver) {
        var tag0 = tag + " [CS]";
        System.out.printf("%s [Invoked]\n", tag0);

        for (Map.Entry<String, Order> orderEntry : orderMap.entrySet()) {
            Order order = orderEntry.getValue();
            int itemsCount = order.getItemsCount();
            for (int index = 0; index < itemsCount; index++) {
                String item = order.getItems(index);
                if (item.contains(request.getS())) {
                    System.out.printf("%s [Found] %s\n",tag0, item);
                    responseObserver.onNext(order);
                    break;
                }
            }
        }
        responseObserver.onCompleted();
        System.out.printf("%s [END]\n\n", tag0);
    }

    // Client Streaming
    @Override
    public StreamObserver<Order> updateOrders(StreamObserver<OrderIdList> responseObserver) {
        var tag0 = tag + " [SS]";
        System.out.printf("%s [Invoked]\n", tag0);
        return new StreamObserver<>() {
            final List<String> orders = new ArrayList<>();

            @Override
            public void onNext(Order ord) {
                if (ord != null) {
                    orderMap.put(ord.getId(), ord);
                    orders.add(ord.getId());
                    System.out.printf("%s [Updated] %s\n", tag0, ord.getId());
                }
            }

            @Override
            public void onError(Throwable t) {
                System.out.printf("%s [Error] %s\n", tag0, t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.printf("%s Update orders - Completed\n", tag0);
                responseObserver.onNext(OrderIdList.newBuilder().addAllIds(orders).build());
                responseObserver.onCompleted();
                System.out.printf("%s [END]\n\n", tag0);
            }
        };
    }


    // Bi-di Streaming
    @Override
    public StreamObserver<OrderId> processOrders(StreamObserver<CombinedShipment> responseObserver) {
        var tag0 = tag + " [BI]";
        System.out.printf("%s [Invoked]\n", tag0);

        return new StreamObserver<>() {
            int batchMarker = 0;
            @Override
            public void onNext(OrderId ord) {
                System.out.printf("%s Order Proc : ID - %s\n", tag0, ord.getId());
                var currentOrder = orderMap.get(ord.getId());
                if (currentOrder == null) {
                    System.out.printf("%s [Not Found] %s\n", tag0, ord.getId());
                    return;
                }
                // Processing an order and increment batch marker to
                batchMarker++;
                var addr = currentOrder.getDestination();
                var shipments = combinedShipmentMap.get(addr);

                if (shipments != null) {
                    shipments = CombinedShipment.newBuilder(shipments).addOrdersList(currentOrder).build();
                    combinedShipmentMap.put(addr, shipments);
                } else {
                    var shipment = CombinedShipment.newBuilder().build();
                    shipment = shipment.newBuilderForType()
                            .addOrdersList(currentOrder)
                            .setId("CMB-" + new Random().nextInt(1000)+ ":" + currentOrder.getDestination())
                            .setStatus("Processed!")
                            .build();
                    combinedShipmentMap.put(currentOrder.getDestination(), shipment);
                }

                if (batchMarker == BATCH_SIZE) {
                    // Order batch completed. Flush all existing shipments.
                    for (var entry : combinedShipmentMap.entrySet()) {
                        responseObserver.onNext(entry.getValue());
                    }
                    // Reset batch marker
                    batchMarker = 0;
                    combinedShipmentMap.clear();
                }
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {
                for (Map.Entry<String, CombinedShipment> entry : combinedShipmentMap.entrySet()) {
                    responseObserver.onNext(entry.getValue());
                }
                responseObserver.onCompleted();
                System.out.printf("%s [END]\n\n", tag0);
            }

        };
    }

}
