### 使用Godeps-Git整合了go的boomer
 
### boomer
 * 是一个基本locust 的压测工具
   + 替换了locust的slave功能
   + 使用时，需要启动locust做为master节点，boomer做为子节点上报压测数据到master
      - 还可以自己实现locust master功能  
 
 * gozmq目前支持的ZMTP3.1，当遇到zmq 通信出现greeting version不一致时，可以手动改源码
   + 出错
   ```` 
    Failed to connect to master(127.0.0.1:5557) with error gomq/zmtp: Got error while receiving greeting: Version 3.0 received does match expected version 3.1
   ````
   + jeromq使用的0.4.3版本
   + 修改
      1. 打开gomq/zmtp/protocol.go
      2. 将minorVersion改为：0
      ``` 
        const (
        	majorVersion uint8 = 3
        	minorVersion uint8 = 0
        ) 
     ```
     
### 测试
 * 运行mail.go，开始向master节点发送数据
 
 
### 调式
 * git clone 下载
 * 设置GOPATH为  xxx/jlocust/go-boomer/.godeps
 * 启动jlocust4j 项目
 * 启动main.go
    1. idea中启动
    2. build main.go启动
       - source gvp: 设置gopath 
       - go build -o main_worker.out
       - ./main_worker.out
 
  
 