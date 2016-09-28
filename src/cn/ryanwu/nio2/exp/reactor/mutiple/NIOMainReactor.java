package cn.ryanwu.nio2.exp.reactor.mutiple;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import javax.xml.ws.Dispatch;

import cn.ryanwu.nio2.exp.utils.SystemUtil;

/**
 * MainReactor只负责监听accept，是否可以直接修改类名为NIOAcceptor
 * key.attach不必要了，因为只有accept，现在的NIOAcceptor没有意义
 * 把NIOAcceptor直接放到run方法中，不用做派发
 * @author wuqiang
 *
 */
public class NIOMainReactor implements Runnable {

	private String host;
	private int port;
	private ServerSocketChannel serverChannel;
	private Selector selector;
	
	private NIOReactorPool reactorPool;
	
	private static boolean stop = false;

	public NIOMainReactor(String host, int port) {
		try {
			reactorPool = new NIOReactorPool("Reactor-Pool", 3);
			
			this.host = host;
			this.port = port;
			this.serverChannel = ServerSocketChannel.open();
			this.serverChannel.configureBlocking(false);
			this.serverChannel
					.bind(new InetSocketAddress(this.host, this.port));
			this.selector = Selector.open();
			SelectionKey key = this.serverChannel.register(this.selector,
					SelectionKey.OP_ACCEPT);
			key.attach(new NIOAcceptor());
			SystemUtil.println("Server is started. listening at port:"
					+ this.port);
		} catch (IOException e) {
			SystemUtil.exitWithErrorMessage("Server start failed.");
		}

	}

	@Override
	public void run() {
		while (!stop) {
			try {
				selector.select();
				Iterator<SelectionKey> iter = selector.selectedKeys()
						.iterator();
				
				while (iter.hasNext()) {
					SelectionKey key = iter.next();
					iter.remove();
					if(key.isValid() && key.isAcceptable()) {
						Runnable acceptor = (Runnable)key.attachment();
						if(acceptor != null) {
							acceptor.run();
						}
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	class NIOAcceptor implements Runnable {

		@Override
		public void run() {
			SocketChannel channel;
			try {
				channel = serverChannel.accept();
				if (channel != null) {
					SystemUtil.println("Get connection from client: "
							+ channel.socket().getInetAddress()
									.getHostAddress() + ", port: "
							+ channel.socket().getPort());
					NIOReactor reactor = reactorPool.getNextReactor(); 
					SystemUtil.println("Register with reactor:" + reactor.getName());
					reactor.register(channel);
					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

}
