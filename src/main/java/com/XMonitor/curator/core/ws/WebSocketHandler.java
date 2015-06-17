package com.XMonitor.curator.core.ws;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.*;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by ksy on 2015/6/16.
 */
public class WebSocketHandler extends SimpleChannelUpstreamHandler {
    private static final Logger log =  LoggerFactory.getLogger(WebSocketHandler.class);

    //websocket链接路径
    private static final String WEBSOCKET_PATH = "/websocket";

    //websocket握手对象
    private WebSocketServerHandshaker handshaker;

    public static ChannelGroup channelGroup = new DefaultChannelGroup();

    private int clientCount = 0;

    public int getClientCount() {
        return clientCount;
    }

    public void setClientCount(int clientCount) {
        this.clientCount = clientCount;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelOpen(ctx, e);
        log.info("开启一个链接：" + ctx.getChannel().getId());
        channelGroup.add(ctx.getChannel());
        this.setClientCount(channelGroup.size());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelConnected(ctx, e);
        log.info("channel已链接上：" + ctx.getChannel().getId());
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelDisconnected(ctx, e);
        log.info("channel已关闭：" + ctx.getChannel().getId());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelClosed(ctx, e);
        log.info("关闭一个链接：" + ctx.getChannel().getId());
        channelGroup.add(ctx.getChannel());
        this.setClientCount(channelGroup.size());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        super.exceptionCaught(ctx, e);
        log.warn("websocket异常:"+e.getCause());
    }

    //接收消息并判断消息类型
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            //首次请求进行握手链接
            handleHttpRequest(ctx, (HttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    //处理http请求
    private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest request){
        //判断是否为hhtp.get请求，并禁止访问
        if(request.getMethod() != GET){
            sendHttpResponse(ctx, request, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }

        //开始握手
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(this.getWebSocketLocation(request), null, false);
        this.handshaker = factory.newHandshaker(request);

        if(handshaker != null){
            this.handshaker.handshake(ctx.getChannel(), request);
//            System.out.println(WebSocketServer.recipients.size());
//            WebSocketServer.recipients.add(ctx.getChannel());
//            System.out.println(WebSocketServer.recipients.size());
//            System.out.println(ctx.getChannel().getId());
        }else{
            factory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
        }
    }

    //处理websocket数据frame
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame){
        //判断frame的类型
        if(frame instanceof CloseWebSocketFrame){
            this.handshaker.close(ctx.getChannel(), (CloseWebSocketFrame)frame);
            return;
        }else if(frame instanceof PingWebSocketFrame){
            ctx.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
        } else if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
                    .getName()));
        }

        String message = ((TextWebSocketFrame)frame).getText();
        log.info("接收到消息:"+message);
        //测试用，直接返回消息内容
        channelGroup.write(new TextWebSocketFrame(message));
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest request, HttpResponse response){
        //请求状态不正常
        if(response.getStatus().getCode() != 200){
            response.setContent(ChannelBuffers.copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8));
            setContentLength(response, response.getContent().readableBytes());
        }

        ChannelFuture future = ctx.getChannel().write(response);
        if(!isKeepAlive(request) || response.getStatus().getCode() != 200){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    //获取请求中的websocket地址
    private String getWebSocketLocation(HttpRequest request) {
        return "ws://" + request.getHeader(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
    }

    public static void pushState(String message){
        if(channelGroup != null && channelGroup.size() > 0) {
            channelGroup.write(new TextWebSocketFrame(message));
        }
    }
}
