package com.moji.zookeepernifty.example;

import org.apache.thrift.TException;

import com.moji.zookeepernifty.DefaultZkNiftyServer;
import com.moji.zookeepernifty.ZkNiftyServerConfig;

public class ServerExample {

	public static void main(String[] args) {
		ZkNiftyServerConfig config = new ZkNiftyServerConfig("ServerExample.xml");
		if (config.load() < 0) {
			System.out.println("Load server configure failed.");
			return;
		}
		HelloService.Iface impl = new HelloService.Iface() {
			public String hello(String username) throws TException {
				return "Hello " + username;
			}
			public String bye(String username) throws TException {
				return "Bye " + username;
			}
		};
		
		DefaultZkNiftyServer server = new DefaultZkNiftyServer(config);
		if (server.init() < 0) {
			System.out.println("DefaultZkNiftyServer initialize failed.");
			return;
		}
		
		try {
			server.start(new HelloService.Processor<>(impl));
			System.out.println("Server start successful.");
		} catch (Exception e) {
			System.out.println("Server start failed. Exception message : " + e.getMessage());
		}
//		
//		try {
//			Thread.sleep(2000000);
//		} catch (Exception e) {
//			System.out.println("Thread sleep exception.");
//		}
//		
//		server.close();
	}
}
