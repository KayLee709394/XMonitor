package com.XMonitor.curator.core.ws;

import com.XMonitor.curator.core.zk.ZookeeperFactory;
import org.apache.curator.framework.CuratorFramework;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * A HTTP server which serves Web Socket requests at:
 * 
 * http://localhost:8080/websocket
 * 
 * Open your browser at http://localhost:8080/, then the demo page will be
 * loaded and a Web Socket connection will be made automatically.
 * 
 * This server illustrates support for the different web socket specification
 * versions and will work with:
 * 
 * <ul>
 * <li>Safari 5+ (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 6-13 (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 14+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * <li>Chrome 16+ (RFC 6455 aka draft-ietf-hybi-thewebsocketprotocol-17)
 * <li>Firefox 7+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * </ul>
 */
public class WebSocketServer {

	private int port;

	private String address;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}


	public WebSocketServer(int port) {
		this.port = port;
	}

	public WebSocketServer(String address, int port) {
		this.address = address;
		this.port = port;
	}

	public void run() {
		//websocket服务配置
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(Executors
						.newCachedThreadPool(), Executors.newCachedThreadPool()));

		// pipelineFactory配置
		bootstrap.setPipelineFactory(new WebSocketPipelineFactory());

		//绑定监听链接的端口
		bootstrap.bind(new InetSocketAddress(address,port));

		System.out.println("Web socket server started at port " + port + '.');
		System.out
				.println("Open your browser and navigate to http://localhost:"
						+ port + '/');
	}

	public static void main(String[] args) {
//		int port;
//		if (args.length > 0) {
//			port = Integer.parseInt(args[0]);
//		} else {
//			port = 8888;
//		}

		try {
			new WebSocketServer("172.18.1.54",8888).run();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}