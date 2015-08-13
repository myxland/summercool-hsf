简介：轻量封装Ibatis3
         因为本人在国内最大的电子商务公司工作期间，深感一个好的分表分库框架可以大大提高系统的承载能力及系统的灵活性，而一个不好的分表分库方案，则让系统在大数据量处理的时候非常郁闷。所以， 在根据笔者在从事电子商务开发的这几年中，对各个应用场景而开发的一个轻量封装Ibatis3的一个分表分库框架。
         笔者工作的这几年之中，总结并开发了如下几个框架： summercool（Web 框架，已经应用于某国内大型网络公司的等重要应用）、summercool-hsf（基于Netty实现的RPC框架，已经应用国内某移动互联网公司）、 summercool-ddl（基于Mybaits的分表分库框架，已经应用国内某移动互联网公司）；相继缓存方案、和消息系统解决方案也会慢慢开源。 Summercool框架做为笔者的第一个开源框架
summercool-hsf：http://summercool-hsf.googlecode.com/svn/trunk
 
 
1. 什么是HSF框架
    HSF框架是一个高性能远程通信框架，底层基于Netty实现TCP通信，对上层进行封装，提供易于使用和高度可扩展能力。



 
 
名词解译：

    1）Channel：可以理解为一个通道，或者连接

    2）ChannelGroup：多个通道组合成为一个ChannelGroup

 

2.HSF工作流程

 
3.消息协议设计
    消息协议这里是指对消息编码和解码的规范的一种定义，HSF内置的消息协议采用如下结构：



 

 
 
    Length：以4个字节表示，是指ID + Content的长度。

    ID：以1个字节表示，1表示Content部分被压缩，0表示未被压缩。

    Content：真实的消息内容。

 

 

4.处理器
    Netty框架原生提供一个处理器链对事件进行处理，每个处理器均实现ChannelHandler接口，ChannelHandler是个空接口，拥有三个子接口：ChannelDownstreamHandler, ChannelUpstreamHandler和LifeCycleAwareChannelHandler。这里我们主要关注前两个接口，因为它们被用来处理读与写的消息。
    事件主要分为三种：ChannelEvent、MessageEvent和ExceptionEvent，一旦这些事件被触发，它们将从处理器链的一端到另一端，被逐个处理器处理，注意，整个过程是单线程场景。一般而言，ChannelEvent和ExceptionEvent事件都是从底层被触发，因此，它们会被ChannelUpstreamHandler处理。而MessageEvent则需要根据读与写方式的不同，分别从两个方向被ChannelUpstreamHandler和ChannelDownstreamHandler处理。
    HSF内置的编(解)码处理器、压缩(解压)处理器及序列化(反序列化)处理器等都是直接或间接实现ChannelHandler。

    ♦ ChannelDownstreamHandler
 
Java代码  收藏代码
public interface ChannelDownstreamHandler extends ChannelHandler {  
    /** 
     * Handles the specified downstream event. 
     * 
     * @param ctx  the context object for this handler 
     * @param e    the downstream event to process or intercept 
     */  
    void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception;  
}  
 
    ♦ ChannelUpstreamHandler
 
Java代码  收藏代码
public interface ChannelUpstreamHandler extends ChannelHandler {  
    /** 
     * Handles the specified upstream event. 
     * 
     * @param ctx  the context object for this handler 
     * @param e    the upstream event to process or intercept 
     */  
    void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception;  
}  
 
 
4.1.Encoding流程
     HSF内置的encoding过程由三个Handler组合完成，流程如下：

 

 
 
    1) SerializeDownstreamHandler

 

Java代码  收藏代码
/** 
 * @Title: SerializeDownstreamHandler.java 
 * @Package com.gexin.hsf.netty.channelhandler.downstream 
 * @Description: 序列化 
 * @author  
 * @date 2011-9-16 下午4:45:59 
 * @version V1.0 
 */  
public class SerializeDownstreamHandler implements ChannelDownstreamHandler {  
    Logger logger = LoggerFactory.getLogger(getClass());  
    private Serializer serializer = new KryoSerializer();  
    public SerializeDownstreamHandler() {  
    }  
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {  
        if (!(e instanceof MessageEvent)) {  
            ctx.sendDownstream(e);  
            return;  
        }  
        MessageEvent event = (MessageEvent) e;  
        Object originalMessage = event.getMessage();  
        Object encodedMessage = originalMessage;  
        if (!(originalMessage instanceof Heartbeat)) {  
            encodedMessage = serializer.serialize(originalMessage);  
        } else {  
            encodedMessage = Heartbeat.BYTES;  
        }  
        if (originalMessage == encodedMessage) {  
            ctx.sendDownstream(e);  
        } else if (encodedMessage != null) {  
            write(ctx, e.getFuture(), encodedMessage, event.getRemoteAddress());  
        }  
    }  
    public void setSerializer(Serializer serializer) {  
        this.serializer = serializer;  
    }  
}  
      2）CompressionDownstreamHandler
 
 

Java代码  收藏代码
/** 
 * @Title: CompressionDownstreamHandler.java 
 * @Package com.gexin.hsf.netty.channelhandler.downstream 
 * @Description: 压缩处理器 
 * @author  
 * @date 2011-9-16 下午4:45:59 
 * @version V1.0 
 */  
public class CompressionDownstreamHandler implements ChannelDownstreamHandler {  
    private CompressionStrategy compressionStrategy = new ThresholdCompressionStrategy();  
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {  
        if (!(e instanceof MessageEvent)) {  
            ctx.sendDownstream(e);  
            return;  
        }  
        MessageEvent event = (MessageEvent) e;  
        Object originalMessage = event.getMessage();  
        if (originalMessage instanceof byte[]) {  
            CompressionResult compressionResult = compressionStrategy.compress((byte[]) originalMessage);  
            byte[] resBuffer = compressionResult.getBuffer();  
            int length = resBuffer.length;  
            byte[] bytes = new byte[length + 1];  
            bytes[0] = compressionResult.isCompressed() ? (byte) 1 : (byte) 0;  
            for (int i = 0; i < length; i++) {  
                bytes[i + 1] = resBuffer[i];  
            }  
            DownstreamMessageEvent evt = new DownstreamMessageEvent(event.getChannel(), event.getFuture(), bytes,  
                    event.getRemoteAddress());  
            ctx.sendDownstream(evt);  
        } else {  
            ctx.sendDownstream(e);  
        }  
    }  
    public void setCompressionStrategy(CompressionStrategy compressionStrategy) {  
        this.compressionStrategy = compressionStrategy;  
    }  
}  
       3）LengthBasedEncoder
 
 

Java代码  收藏代码
/** 
 * @ClassName: LengthBasedEncoder 
 * @Description: 基于长度的编码器 
 * @author  
 * @date 2011-9-29 下午1:43:41 
 *  
 */  
public class LengthBasedEncoder extends ObjectEncoder {  
    Logger logger = LoggerFactory.getLogger(getClass());  
    private final int estimatedLength;  
    public LengthBasedEncoder() {  
        this(512);  
    }  
    public LengthBasedEncoder(int estimatedLength) {  
        if (estimatedLength < 0) {  
            throw new IllegalArgumentException("estimatedLength: " + estimatedLength);  
        }  
        this.estimatedLength = estimatedLength;  
    }  
    @Override  
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {  
        if (msg instanceof byte[]) {  
            byte[] bytes = (byte[]) msg;  
            ChannelBuffer ob = ChannelBuffers.dynamicBuffer(estimatedLength, channel.getConfig().getBufferFactory());  
            ob.writeInt(bytes.length);  
            ob.writeBytes(bytes);  
            return ob;  
        } else {  
            throw new IllegalArgumentException("msg must be a byte[], but " + msg);  
        }  
    }  
}  
 
 

   4.2.Decoding流程
    decoding流程与encoding正好相反，流程如下：

 


    1）LengthBasedDecoder

    对于TCP通信而言，粘包是很正常的现象，因此decoder必须处理粘包问题。HsfFrameDecoder是一个支持粘包处理的decoder类抽象。

Java代码  收藏代码
/** 
 * @ClassName: LengthBasedDecoder 
 * @Description: 基于长度的解码器 
 * @author  
 * @date 2011-9-29 下午1:42:59 
 *  
 */  
public class LengthBasedDecoder extends HsfFrameDecoder {  
    private Logger logger = LoggerFactory.getLogger(getClass());  
    private int headerFieldLength = 4;  
    public LengthBasedDecoder() {  
        this(4);  
    }  
    public LengthBasedDecoder(int headerFieldLength) {  
        this.headerFieldLength = headerFieldLength;  
    }  
    @Override  
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {  
        if (buffer.readableBytes() >= headerFieldLength) {  
            buffer.markReaderIndex();  
            int length = buffer.readInt();  
            if (length < 0) {  
                logger.error("msg length must >= 0. but length={}", length);  
                return null;  
            } else if (length == 0) {  
                return Heartbeat.BYTES;  
            } else if (buffer.readableBytes() >= length) {  
                byte[] bytes = new byte[length];  
                buffer.readBytes(bytes);  
                return bytes;  
            } else {  
                buffer.resetReaderIndex();  
            }  
        }  
        return null;  
    }  
}  
     2）DecompressionUpstreamHandler

Java代码  收藏代码
/** 
 * @Title: DecompressionUpstreamHandler.java 
 * @Package com.gexin.hsf.netty.channelhandler.downstream 
 * @Description: 解压缩处理器 
 * @author  
 * @date 2011-9-16 下午4:45:59 
 * @version V1.0 
 */  
public class DecompressionUpstreamHandler extends SimpleChannelUpstreamHandler {  
    private CompressionStrategy compressionStrategy = new ThresholdCompressionStrategy();  
    @Override  
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {  
        if (e.getMessage() instanceof byte[]) {  
            byte[] bytes = (byte[]) e.getMessage();  
            int length = bytes.length;  
            if (length > 0) {  
                byte[] buffer = new byte[length - 1];  
                for (int i = 1; i < length; i++) {  
                    buffer[i - 1] = bytes[i];  
                }  
                if (bytes[0] == 1) {  
                    buffer = compressionStrategy.decompress(buffer);  
                }  
                UpstreamMessageEvent event = new UpstreamMessageEvent(e.getChannel(), buffer, e.getRemoteAddress());  
                super.messageReceived(ctx, event);  
            }  
        } else {  
            super.messageReceived(ctx, e);  
        }  
    }  
    public void setCompressionStrategy(CompressionStrategy compressionStrategy) {  
        this.compressionStrategy = compressionStrategy;  
    }  
}  
     3）DeserializeUpstreamHandler

Java代码  收藏代码
/** 
 * @Title: DeserializeUpstreamHandler.java 
 * @Package com.gexin.hsf.netty.channelhandler.downstream 
 * @Description: 反序列化 
 * @author  
 * @date 2011-9-16 下午4:45:59 
 * @version V1.0 
 */  
public class DeserializeUpstreamHandler extends SimpleChannelUpstreamHandler {  
    private Logger logger = LoggerFactory.getLogger(getClass());  
    private Serializer serializer = new KryoSerializer();  
    @Override  
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {  
        if (e.getMessage() == null) {  
            return;  
        } else if (e.getMessage() instanceof byte[]) {  
            byte[] bytes = (byte[]) e.getMessage();  
            Object msg;  
            if (bytes.length == 0) {  
                msg = Heartbeat.getSingleton();  
            } else {  
                try {  
                    msg = serializer.deserialize(bytes);  
                } catch (Exception ex) {  
                    throw ex;  
                }  
            }  
            UpstreamMessageEvent event = new UpstreamMessageEvent(e.getChannel(), msg, e.getRemoteAddress());  
            super.messageReceived(ctx, event);  
        } else {  
            super.messageReceived(ctx, e);  
        }  
    }  
    public void setSerializer(Serializer serializer) {  
        this.serializer = serializer;  
    }  
}  
    4.3.处理器链的建立
    HSF使用如下的方式构建处理器链：

Java代码  收藏代码
bootstrap.setPipelineFactory(new ChannelPipelineFactory() {  
            public ChannelPipeline getPipeline() throws Exception {  
                ChannelPipeline pipeline = Channels.pipeline();  
                // 注册各种自定义Handler  
                for (String key : handlers.keySet()) {  
                    pipeline.addLast(key, handlers.get(key));  
                }  
                // 注册链路空闲检测Handler  
                Integer writeIdleTime = LangUtil.parseInt(options.get(HsfOptions.WRITE_IDLE_TIME));  
                Integer readIdleTime = LangUtil.parseInt(options.get(HsfOptions.READ_IDLE_TIME));  
                if (writeIdleTime == null) {  
                    writeIdleTime = 10;  
                }  
                if (readIdleTime == null) {  
                    // 默认为写空闲的3倍  
                    readIdleTime = writeIdleTime * 3;  
                }  
                pipeline.addLast("timeout", new IdleStateHandler(idleTimer, readIdleTime, writeIdleTime, 0));  
                pipeline.addLast("idleHandler", new StateCheckChannelHandler(HsfAcceptorImpl.this));  
                // 注册事件分发Handler  
                pipeline.addLast("dispatchHandler", new DispatchUpStreamHandler(eventDispatcher));  
                return pipeline;  
            }  
        });  
5.Dispatcher
    消息经过Handler链处理后，将被Dispatcher转发，并进入EventListener链处理。

    Dispatcher内置两个线程池：channelExecutor和msgExecutor。

    channelExecutor用于处理通道事件和异常事件，考虑到在通道事件可能需要同步调用远程服务，因此此线程池不设上线（因为同步调用将会阻塞当前线程）。

    msgExecutor用于处理消息事件，根据经验值，缺省最大线程数为150，该值可以通过Option参数修改。

6.EventListener
    EventListener有以下三种：

    1）ChannelEventListener

Java代码  收藏代码
/** 
 * @Title: ChannelEventListener.java 
 * @Package com.gexin.hsf.netty.listener 
 * @Description: 通道事件监听类 
 * @author  
 * @date 2011-9-27 上午11:45:50 
 * @version V1.0 
 */  
public interface ChannelEventListener extends EventListener {  
    /** 
     * Invoked when a {@link Channel} was closed and all its related resources were released. 
     *  
     * @author 
     * @param ctx 
     * @param channel 
     * @param e 
     * @return EventBehavior Whether to continue the events deliver 
     */  
    public EventBehavior channelClosed(ChannelHandlerContext ctx, HsfChannel channel, ChannelStateEvent e);  
    /** 
     * Invoked when a {@link Channel} is open, bound to a local address, and connected to a remote address. 
     *  
     * @author 
     * @param ctx 
     * @param channel 
     * @param e 
     * @return EventBehavior Whether to continue the events deliver 
     */  
    public EventBehavior channelConnected(ChannelHandlerContext ctx, HsfChannel channel, ChannelStateEvent e);  
    /** 
     * Invoked when a group is created. 
     *  
     * @author 
     * @param ctx 
     * @param channel 
     * @param groupName 
     * @return EventBehavior Whether to continue the events deliver 
     */  
    public EventBehavior groupCreated(ChannelHandlerContext ctx, HsfChannel channel, String groupName);  
    /** 
     * Invoked when a group is removed. 
     *  
     * @author 
     * @param ctx 
     * @param channel 
     * @param groupName 
     * @return EventBehavior Whether to continue the events deliver 
     */  
    public EventBehavior groupRemoved(ChannelHandlerContext ctx, HsfChannel channel, String groupName);  
}  
     2）MessageEventListener

Java代码  收藏代码
/** 
 * @Title: MessageListener.java 
 * @Package com.gexin.hsf.netty.listener 
 * @Description: 消息监听接口 
 * @author 
 * @date 2011-9-27 上午11:36:22 
 * @version V1.0 
 */  
public interface MessageEventListener extends EventListener {  
    /** 
     * Invoked when a message object (e.g: {@link ChannelBuffer}) was received 
     * from a remote peer. 
     */  
    public EventBehavior messageReceived(ChannelHandlerContext ctx, HsfChannel channel, MessageEvent e);  
}  
     3）ExceptionEventListener

Java代码  收藏代码
/** 
 * @Title: ExceptionEventListener.java 
 * @Package com.gexin.hsf.netty.listener 
 * @Description: 异常监听接口 
 * @author 
 * @date 2011-9-27 上午11:48:09 
 * @version V1.0 
 */  
public interface ExceptionEventListener extends EventListener {  
    /** 
     * Invoked when an exception was raised by an I/O thread or a {@link ChannelHandler}. 
     */  
    public EventBehavior exceptionCaught(ChannelHandlerContext ctx, Channel channel, ExceptionEvent e);  
}  
     Hsf框架会预先在EventListener链末端注册ServiceMessageEventListener，该Listener负责调用被注册的Service，并将返回值或异常回传。

    7.Service
    1）RemoteServiceContract注解

    所有实现了拥有RemoteServiceContract注解的Java类都可以直接注册到HsfService，示例如下：

Java代码  收藏代码
@RemoteServiceContract  
public interface TestService {  
    String test(String ctx);  
}  
   
public class TestServiceImpl implements TestService {  
    @Override  
    public String test(String ctx) {  
        return String.valueOf("hello " + ctx);  
    }  
}  
    2）ServiceEntry

    对于未添加RemoteServiceContract注解的接口，Hsf框架使用org.summercool.hsf.pojo.ServiceEntry类实现注册。

   

    3）注册Service

    服务提供方需要向Hsf注册Service方可被远程调用，示例如下：

    ♦ 注册Service

Java代码  收藏代码
HsfAcceptor acceptor = new HsfAcceptorImpl();  
// 注册Service  
acceptor.registerService(new TestServiceImpl());  
// 监听端口  
acceptor.bind(8082);  
     ♦ 远程调用Service

Java代码  收藏代码
HsfConnector connector = new HsfConnectorImpl();  
connector.connect(new InetSocketAddress("127.0.0.1",8082));  
// 同步方式  
TestService testService = ServiceProxyFactory.getRoundFactoryInstance(connector).wrapSyncProxy(TestService.class);  
System.out.println(testService.test("HSF"));  
    

    3）同步与异步

    4）原理

   7.Handshake
    当通道建立后，Client和Server会进行三次握手，以完成初始化



    初次握手步骤

    1）Client与Server建立连接成功

    2）Client向Server发送握手请求包(handshake request)

    3）Server接收到握手请求包后，生成group信息，然后触发groupCreated事件，接着向client发送握手反馈包(handshake ack)

    4）Client接收到握手反馈包后，生成group信息，然后触发groupCreated事件，接着向server发送握手完成包(handshake finish)

    非初次握手步骤

    1）Client与Server建立连接成功

    2）Client向Server发送握手请求包(handshake request)

    3）Server接收到握手请求包后，添加该连接到Group，接着向client发送握手反馈包(handshake ack)

    4）Client接收到握手反馈包后，添加该连接到Group，接着向server发送握手完成包(handshake finish)

    以上三次握手所发送的包都只包含本身的group信息，但Hsf对外提供了握手的扩展接口，应用可以使用该接口结合自身的业务，以完成连接建立后的初始化工作。

    Client握手扩展接口

   8.Heartbeat、超时及重连机制
    Heartbeat和超时机制依赖于Netty的读空闲和写空闲回调。

    当发生写空闲时，会向对方发送Heartbeat消息，写空闲时间可以通过参数HsfOptions.WRITE_IDLE_TIME设定，缺省为10秒。

    当发生读空闲时，即判定为超时，主动关闭连接，读空闲时间可以通过参数HsfOptions.READ_IDLE_TIME设定，缺省为60秒。

    对于断开的连接，Hsf会为其重连，重连频率通过HsfOptions.RECONNECT_INTERVAL参数设定，缺省为10000毫秒。

 
   9.Option参数
    Hsf支持以参数配置：

参数名 说明 缺省值
HsfOptions.TCP_NO_DELAY	TCP参数，是否关闭延迟发送消息包	true
HsfOptions.KEEP_ALIVE	TCP参数，是否保持连接	true
HsfOptions.REUSE_ADDRESS	TCP参数，是否重用端口	false
HsfOptions.WRITE_IDLE_TIME	写空闲时间(秒)	10
HsfOptions.READ_IDLE_TIME	读空闲时间(秒)	60
HsfOptions.SYNC_INVOKE_TIMEOUT	同步调用超时时间(毫秒)	60000
HsfOptions.HANDSHAKE_TIMEOUT	握手超时时间(毫秒)	15000
HsfOptions.FLOW_LIMIT	流量限额	2000000
HsfOptions.TIMEOUT_WHEN_FLOW_EXCEEDED	申请流量超时时间(毫秒)	3000
HsfOptions.MAX_THREAD_NUM_OF_DISPATCHER	分发器的最大线程数	150
HsfOptions.CHANNEL_NUM_PER_GROUP	每个Group建立的通道数	Runtime.getRuntime().availableProcessors()
HsfOptions.RECONNECT_INTERVAL	重连频率(毫秒)	10000
HsfOptions.CONNECT_TIMEOUT	建立连接超时时间(毫秒)	30000
HsfOptions.HOLD_CALLBACK_MESSAGE	是否缓存Callback方式发送的消息，缓存后将会在发送失败时回调doException方法参数传入	
false

    这些参数可以通过如下方式调整：

Java代码  收藏代码
HsfConnector connector = new HsfConnectorImpl();  
connector.setOption(HsfOptions.CHANNEL_NUM_PER_GROUP, 1);  
10.Hsf的使用
