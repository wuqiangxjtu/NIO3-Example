package cn.ryanwu.nio2.exp.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import cn.ryanwu.nio2.exp.utils.SystemUtil;

public class NioServer {
	
	public static int PORT = 1235;
	
	public static int CAPACITY = 1024;
	
	public static void main(String[] args) {
		NioEchoServer server = new NioEchoServer(PORT);
		new Thread(server).start();
	}
	
	public static class NioEchoServer implements Runnable{
		private Selector selector;
		private ServerSocketChannel serverChannel;
		
		private volatile boolean stop;
		
		public NioEchoServer(int port) {
			try {
				//打开选择器
				this.selector = Selector.open();
				//打开ServerSocketChannel
				this.serverChannel = ServerSocketChannel.open();
				//设置连接为非阻塞模式
				this.serverChannel.configureBlocking(false); 
				//绑定address
				this.serverChannel.socket().bind(new InetSocketAddress(PORT),128);
				//把accept事件注册到selector上
				this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
				
				System.out.println("The Echo server is start in port : " + port);
			} catch (IOException e) {
				e.printStackTrace();
				SystemUtil.exitWithErrorMessage("Start server failed. ");
			}
			
		}

		@Override
		public void run() {
			while(!stop) {
				SelectionKey key = null;
				try {
					selector.select(2000);
					Set<SelectionKey> keys = selector.selectedKeys();
					Iterator<SelectionKey> it = keys.iterator();
					
					while(it.hasNext()) {
						key = it.next();
						//从迭代器里删除，如果不删除会怎么样？
						it.remove();
						handle(key);
					}
				} catch (IOException e) {
					e.printStackTrace();
					
				} 
			}
			
		}
		
		public void handle(SelectionKey key) throws IOException {
			if(key.isValid()) {
				//处理accept
				if(key.isAcceptable()) {
					ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
					SocketChannel socket = ssc.accept();
					socket.configureBlocking(false);
					socket.register(selector, SelectionKey.OP_READ);
					System.out.println("client connect at port: " + socket.socket().getPort());
				}
				
				//处理read
				if(key.isReadable()) {
					SocketChannel sc = (SocketChannel)key.channel();
					
					ByteBuffer buffer = ByteBuffer.allocate(CAPACITY);
					int num = sc.read(buffer);
					if(num > 0) {
						buffer.flip();
						byte[] messageBytes = new byte[buffer.remaining()];
						buffer.get(messageBytes);
						String message = new String(messageBytes, "UTF-8");
						System.out.println("Get message:" + message);
						doWrite(sc,message);
					}else if(num < 0) {
						key.cancel();
						sc.close(); //这里为什么要close?
					}

				}
			}
		}
		
		private void doWrite(SocketChannel channel, String message) throws IOException {
			byte[] messageBytes = message.getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(messageBytes.length);
			buffer.put(messageBytes).flip();
			channel.write(buffer);
		}
	}
	



}
