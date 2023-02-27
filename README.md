# gRPC-Up-and-Running-Java

## jdk version
17+

## gradle version
```shell
./gradlew -version                                                                                                                                                                             ─╯
```
output:
```shell

------------------------------------------------------------
Gradle 8.0.1
------------------------------------------------------------

Build time:   2023-02-17 20:09:48 UTC
Revision:     68959bf76cef4d28c678f2e2085ee84e8647b77a

Kotlin:       1.8.10
Groovy:       3.0.13
Ant:          Apache Ant(TM) version 1.10.11 compiled on July 10 2021
JVM:          19.0.2 (Oracle Corporation 19.0.2+7-44)
OS:           Mac OS X 13.2.1 aarch64

```

## project setup
refer to [settings.gradle](https://github.com/byegates/gRPC-Up-and-Running-Java/blob/master/settings.gradle) file, two modules: server and client.

## build server
```shell
./gradlew server:build
```

## build client
```shell
./gradlew client:build
```

### generate proto only
```shell
./gradlew generateProto 
```

## run server
runs on port 50071, you can change it in source code as needed
```shell
java -jar server/build/libs/server-1.0-SNAPSHOT.jar
```
After above command, you should see:
```shell
[Server] [started] listening on: 50071
```
## run client
```shell
java -jar client/build/libs/client-1.0-SNAPSHOT.jar
```
After above command, you should see(Product ID will be different, but in UUID format):
```shell
[Client] [C] [Invoked]
[Client] [C] [SUCCESS] Product ID: 4261cfa3-d841-45cd-b500-880bcb3c7848
[Client] [C] [END]

[Client] [R] [Invoked]
[Client] [R] 
name: "Apple iPhone 14 Pro Max"
description: "Pro. Beyond."
price: 1099.0
[Client] [R] [END]

[Client] [SS] [Invoked]
[Client] [SS] Order :
id: "102"
items: "Google Pixel 3A"
items: "Mac Book Pro"
price: 1800.0
destination: "Mountain View, CA"
[Client] [SS] Order :
id: "104"
items: "Google Home Mini"
items: "Google Nest Hub"
price: 400.0
destination: "Mountain View, CA"
[Client] [SS] [END]

[Client] [CS] [Invoked]
[Client] [CS] Update Orders :
102, 103, 104, 104, 
[Client] [CS] Update orders response completed!
[Client] [CS] [END]

[Client] [BI] [Invoked]
[Client] [BI] Combined Shipment :
CMB-220:San Jose, CA :
[id: "103"
items: "Apple Watch S4"
items: "Mac Book Pro"
items: "iPad Pro"
price: 2800.0
destination: "San Jose, CA"
]
[Client] [BI] Combined Shipment :
CMB-406:Mountain View, CA :
[id: "102"
items: "Google Pixel 3A"
items: "Google Pixel Book"
price: 1100.0
destination: "Mountain View, CA"
, id: "104"
items: "Google Home Mini"
items: "Google Nest Hub"
items: "iPad Mini"
price: 2200.0
destination: "Mountain View, CA"
]
[Client] [BI] Order Processing completed!
[Client] [BI] [END]
```
