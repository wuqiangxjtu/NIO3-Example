package cn.ryanwu.nio2.exp.reactor.single;


public class NIOServer {
	
	public static int PORT = 1236;
	
	public static int CAPACITY = 1024;
	
	public static void main(String[] args) {
		NIOReactor reactor = new NIOReactor("127.0.0.1", PORT);
		new Thread(reactor).start();
	}
	


}
