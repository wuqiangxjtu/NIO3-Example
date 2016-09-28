package cn.ryanwu.nio2.exp.utils;

public class SystemUtil {
	
	public static void exitWithErrorMessage(String message) {
		System.out.println("Exit -1, message:" + message);
		System.exit(-1);
	}
	
	public static void println(String message) {
		System.out.println(message);
	}

}
