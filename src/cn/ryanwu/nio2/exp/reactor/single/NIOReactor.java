package cn.ryanwu.nio2.exp.reactor.single;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NIOReactor implements Runnable {

	private ServerSocketChannel serverChannel;
	private Selector selector;
	private volatile boolean stop = false;

	private String host;
	private int port;

	public NIOReactor(String host, int port) {
		try {
			this.host = host;
			this.port = port;
			this.serverChannel = ServerSocketChannel.open();
			this.serverChannel.configureBlocking(false);
			this.selector = Selector.open();
			this.serverChannel.bind(new InetSocketAddress(this.host, this.port));
			SelectionKey key = this.serverChannel.register(this.selector,
					SelectionKey.OP_ACCEPT);
			key.attach(new NIOAcceptor());
			System.out.println("Server is started. listening at port:"
					+ this.port);
		} catch (IOException e) {
			System.out.println("Server start fail. ");
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			while(!stop) {
				
				selector.select();
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey key = it.next();
					it.remove();
					dispatch((Runnable) key.attachment());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void dispatch(Runnable runnable) {
		if (runnable != null) {
			runnable.run();
		}
	}

	/**
	 * 
	 * @author wuqiang
	 *
	 */
	class NIOAcceptor implements Runnable {
		@Override
		public void run() {
			try {
				SocketChannel channel = serverChannel.accept();
				if(channel != null) {
					System.out.println("client connected. Port:" + channel.socket().getPort());
					new Handler(channel);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 
	 * @author wuqiang
	 *
	 */
	class Handler implements Runnable {
		
		private static final int READ_STATUS = 1;
		private static final int WRITE_STATUS = 2; 
		
		private final static int CAPACITY = 1024;
		
		private SelectionKey selectionKey;
		private SocketChannel channel;
		
		private int status = READ_STATUS;
		
		byte[] message;
		
		public Handler(SocketChannel channel) {
			try {
				this.channel = channel;
				channel.configureBlocking(false);
				selectionKey = channel.register(selector, SelectionKey.OP_READ);
				selectionKey.attach(this);
				selector.wakeup();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		@Override
		public void run() {
			
			if(selectionKey.isValid()) {
				if (status == READ_STATUS && selectionKey.isReadable()) {
					try {
						//输入
						message = read();
						//处理
						message = process(message);
						selectionKey.interestOps(SelectionKey.OP_WRITE);
						status = WRITE_STATUS;
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (status == WRITE_STATUS && selectionKey.isWritable()) {
					try {
						//输出
						write(message);
						System.out.println("Sever send message successful.");
						selectionKey.interestOps(SelectionKey.OP_READ);
						status = READ_STATUS;
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
				
			}
			
		}
		
		private byte[] read() throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(CAPACITY);
			int num = channel.read(buffer);
			byte[] messageBytes = null;
			if(num > 0) {
				buffer.flip();
				messageBytes = new byte[buffer.remaining()];
				buffer.get(messageBytes);
				String message = new String(messageBytes, "UTF-8");
				System.out.println("Get message:" + message);
			}else if(num < 0) {
				selectionKey.cancel();
//				channel.close();
			}
			return messageBytes;
		}
		
		private void write(byte[] content) throws IOException {
	        ByteBuffer buffer = ByteBuffer.wrap(content);  
	        channel.write(buffer);  
		}
		
		private byte[] process(byte[] input) throws UnsupportedEncodingException {
			String content = new String(input,"UTF-8");
			content = "Add server tag -> " + content;
			return content.getBytes();
		}
	}
	


}