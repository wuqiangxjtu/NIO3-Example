package cn.ryanwu.nio2.exp.reactor.mutiple;


public class NIOServer {
	public static int PORT = 1238;
	
	public static int CAPACITY = 1024;
	
	public static void main(String[] args) {
		NIOMainReactor acceptor = new NIOMainReactor("127.0.0.1", PORT);
		new Thread(acceptor).start();
	}
}
