package com.moji.zookeepernifty.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moji.zookeepernifty.ZkNiftyClientConfig;
import com.moji.zookeepernifty.ZkNiftyTransportManager;
import com.moji.zookeepernifty.ZkNiftyTransportManager.TProtocolWithType;
import com.moji.zookeepernifty.example.HelloService;

public class ClientExample {

	private static ZkNiftyClientConfig config;
	private static final Logger log = LoggerFactory.getLogger(ClientExample.class);
	
	private static void runWithSingleThread() {
		final ZkNiftyTransportManager manager = ZkNiftyTransportManager.getInstance(config);
		final String path = "/HelloService/1.0.0";
		// 必须注册服务路径对应的Client类
		manager.registerClientClass(path, HelloService.Client.class);
		manager.run();
		
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			System.out.print("Exception : " + e.getMessage());
		}
		
		long startTime = System.currentTimeMillis(); // 获取开始时间
		
		for (int i = 0; i < 100000; ++i) {
			TProtocolWithType protocol = manager.getTransport(path);
			if (protocol != null) {

				HelloService.Client client = new HelloService.Client(protocol.getProtocol());
				try {
					String echo = client.hello("World.");
					log.debug(echo);
					// Thread.sleep(1000);
					echo = client.bye("World.");
					//System.out.println(echo + i);
					log.debug("{} {}", echo, i);
				} catch (Exception e) {
					System.out.println("Talk Failed. Exception : " + e.getMessage());
				}
			}
			manager.putTransport(path, protocol);
		}
	
		
		long endTime=System.currentTimeMillis(); //获取结束时间  
	    System.out.println("程序运行时间： "+(endTime-startTime)+"ms");  
		
		manager.stop();
	}
	
	
	public static void main(String[] args) {
		config = new ZkNiftyClientConfig("ClientExample.xml");
		if (config.load() < 0) {
			System.out.println("Load configure failed.");
			return;
		}
		runWithSingleThread();
	}
}
