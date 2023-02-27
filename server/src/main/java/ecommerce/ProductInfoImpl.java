package ecommerce;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ecommerce.Server.tag;

public class ProductInfoImpl extends ProductInfoGrpc.ProductInfoImplBase {
    private final Map<String, Product> productMap = new HashMap<>();

    @Override
    public void addProduct(Product request, StreamObserver<ProductID> responseObserver) {
        String tag0 = tag + " [C]";
        System.out.printf("%s [Invoked]\n", tag0);
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();

        productMap.put(uuidString, request);
        ProductID id = ProductID.newBuilder().setValue(uuidString).build();

        responseObserver.onNext(id);
        responseObserver.onCompleted();
        System.out.printf("%s [END]\n\n", tag0);
    }

    @Override
    public void getProduct(ProductID request, StreamObserver<Product> responseObserver) {
        String tag0 = tag + " [R]";
        System.out.printf("%s [Invoked]\n", tag0);
        String id = request.getValue();
        var product = productMap.get(id);

        if (product != null) {
            responseObserver.onNext(product);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
        }
        System.out.printf("%s [END]\n\n", tag0);
    }
}
