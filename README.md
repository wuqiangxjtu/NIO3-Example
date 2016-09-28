### NIO以及Reactor示例

#### (示例程序，不健壮，不能用于生产环境)

+ cn.ryanwu.nio2.exp.bio:阻塞IO
+ cn.ryanwu.nio2.exp.nio:NIO
+ cn.ryanwu.nio2.exp.reactor.single:只有一个Reactor，单线程监听accept，read，write，进行read，write等io操作，并且承担process(decode，encode等等)工作，
+ cn.ryanwu.nio2.exp.reactor.workerpool:只有一个Reactor，单线程进行accept，read，write工作，process由线程池多线程进行处理
+ cn.ryanwu.nio2.exp.reactor.mutiple:一个main Reactor做为Acceptor，接收请求；一个subReactor池，监听read，writer；process由线程池多线程进行处理。有可能需要同步