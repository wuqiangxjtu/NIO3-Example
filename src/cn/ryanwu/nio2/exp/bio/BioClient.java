package cn.ryanwu.nio2.exp.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class BioClient {
	
	public static int PORT = 1234;
	
	public static void main(String[] args) {
		Socket socket = null;
		BufferedReader br = null;
		PrintWriter pw = null;
		BufferedReader input = null;
		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress("127.0.0.1", PORT));
			input = new BufferedReader(new InputStreamReader(System.in));
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			pw = new PrintWriter(socket.getOutputStream(),true);
			
			String userInput = null;
			while((userInput = input.readLine()) != null) {
				pw.println(userInput);
				System.out.println("返回:" + br.readLine());
			}
			pw.close();
			br.close();
			socket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

}
