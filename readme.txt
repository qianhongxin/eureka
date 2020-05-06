1. 目录介绍
    eureka-client：客户端
    eureka-client-archaius2：客户端与archaius2配置中心的交互
    eureka-client-jersey2：客户端与服务端的通信代码，基于jersey2
    eureka-core：服务端逻辑
    eureka-core-jersey2：服务端集群间通信代码，基于jersey2
    eureka-examples：使用案例
    eureka-resources：eureka的页面代码
    eureka-server：服务端部署web文件

2. 客户端代码入口阅读：
    eureka-examples的ExampleEurekaClient

3. 服务端代码入口阅读：
    eureka-core的EurekaBootStrap