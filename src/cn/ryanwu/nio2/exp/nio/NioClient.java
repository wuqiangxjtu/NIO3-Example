package cn.ryanwu.nio2.exp.nio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import cn.ryanwu.nio2.exp.utils.SystemUtil;

public class NioClient {
	
	public static int PORT = 1235;
	
	public static void main(String[] args) {
		new Thread(new NioEchoClient("localhost", PORT)).start();
	}
	
	public static class NioEchoClient implements  Runnable{
		
		public static int CAPACITY = 1024;
		
		private Selector selector;
		private SocketChannel socketChannel;
		private String host;
		private int port;
		
		private volatile boolean stop = false;
				
		public NioEchoClient(String host, int port) {
			try {
				this.host = host;
				this.port = port;
				this.selector = Selector.open();
				this.socketChannel = SocketChannel.open();
				this.socketChannel.configureBlocking(false);
			} catch (IOException e) {
				e.printStackTrace();
				SystemUtil.exitWithErrorMessage("Failed to config socket channel.");
			}
			
		}

		@Override
		public void run() {
			new MessageThread().start();
			try {
				doConnect();
			} catch (IOException e) {
				e.printStackTrace();
				SystemUtil.exitWithErrorMessage("Failed to connect to server.");
			}
			SelectionKey key = null;
			while(!stop) {
				try {
					selector.select();
					Set<SelectionKey> keys = selector.selectedKeys();
					Iterator<SelectionKey> it = keys.iterator();
					while(it.hasNext()) {
						key = it.next();
						it.remove();
						handle(key);
					}
				} catch (IOException e) {
					if(key != null) {
						key.cancel();
						if(key.channel() != null) {
							try {
								key.channel().close();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
					e.printStackTrace();
				}
				
			}
			
			if(this.selector != null) {
				try {
					selector.close();
				} catch (IOException e) {
					e.printStackTrace();
					SystemUtil.exitWithErrorMessage("Not regular stop.");
				}
			}
		}
		
		private void doConnect() throws IOException {
			if(this.socketChannel.connect(new InetSocketAddress(host, port))) {
				this.socketChannel.register(this.selector, SelectionKey.OP_READ);
			}else {
				this.socketChannel.register(this.selector, SelectionKey.OP_CONNECT);
			}
			
		}
		
		private void doWrite(SocketChannel channel, String message) throws IOException {
			byte[] messageBytes = message.getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(messageBytes.length);
			buffer.put(messageBytes);
			buffer.flip();
			channel.write(buffer);
		}
		
		private void handle(SelectionKey key) throws ClosedChannelException, IOException {
			if(key.isValid()) {
				SocketChannel sc = (SocketChannel)key.channel();
				if(key.isConnectable()) {
					if(sc.finishConnect()) {
						sc.register(selector, SelectionKey.OP_READ);
					}else {
						System.exit(-1);
					}
				}
				if(key.isReadable()) {
					ByteBuffer buffer = ByteBuffer.allocate(CAPACITY);
					int num = sc.read(buffer);
					if(num > 0) {
						buffer.flip();
						byte[] messageBytes = new byte[buffer.remaining()];
						buffer.get(messageBytes);
						String message = new String(messageBytes, "UTF-8");
						System.out.println("Get Echo:" + message);
					}

				}
			}
		}
		
		public class MessageThread extends Thread {
			@Override
			public void run() {
				try {
					BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
					String inputMessage = null;
					while((inputMessage = input.readLine()) != null) {
						doWrite(socketChannel, inputMessage);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	
	}
	

  

}
