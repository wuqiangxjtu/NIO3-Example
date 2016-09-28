package cn.ryanwu.nio2.exp.reactor.mutiple;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



public class NIOReactor implements Runnable{
	
	private Selector selector;
	private volatile Boolean stop = false; 
	
	private Object gate = new Object();
	
	private String name;


	public NIOReactor(String name) {
		this.name = name;
	}

	@Override
	public void run() {
		
		try {
			
			this.selector = Selector.open();
			while(true) {
				synchronized (gate) {} //防止直接进入select
				
				selector.select();
					
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				SelectionKey key = null;
				while(it.hasNext()) {
					key = it.next();
					it.remove();
					dispatch((Runnable)key.attachment());
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	void register(SocketChannel channel)  {
		try{
			channel.configureBlocking(false);
			synchronized (gate) {
				selector.wakeup();
				SelectionKey selectionKey = channel.register(this.selector,
						SelectionKey.OP_READ);
				selectionKey.attach(new Handler(channel, this.selector));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void dispatch(Runnable runnable) {
		if (runnable != null) {
			runnable.run();
		}
	}
	
	
	public Boolean getStop() {
		return stop;
	}

	public void setStop(Boolean stop) {
		this.stop = stop;
	}
	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	

	/**
	 * 
	 * @author wuqiang
	 *
	 */
	 static class Handler implements Runnable {
		
		private static final int READ_STATUS = 1;
		private static final int WRITE_STATUS = 2; 
		private static final int PROCESSING_STATUS = 3;
		
		private final static int CAPACITY = 1024;
		
		//worker线程池
		private static final ExecutorService workers =  Executors.newFixedThreadPool(10);
		
		private SelectionKey selectionKey;
		private SocketChannel channel;
		
		private int status = READ_STATUS;
		
		byte[] message;
		
		public Handler(SocketChannel channel, Selector selector) {
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
						//使用callable和线程池
						status = PROCESSING_STATUS;
						Processor processor = new Processor(message);
						Future<byte[]> future = workers.submit(processor);
						message = future.get();

					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
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
		
		
		
		/**
		 * 带有返回值的线程
		 * @author wuqiang
		 *
		 */
		class Processor implements Callable<byte[]> {
			
			private byte[] input;
			
			public Processor(byte[] input) {
				this.input = input;
			}

			@Override
			public byte[] call() throws Exception {				
//				Thread.sleep(30000); 阻塞，占满线程池，看是否还能够处理
				String content = null;
				try {
					selectionKey.interestOps(SelectionKey.OP_WRITE);
					status = WRITE_STATUS;
					
					content = new String(this.input,"UTF-8");
					content = "Add server tag -> " + content;
					
					return content.getBytes(); 
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return null;
			}
		}
		
	}


}
