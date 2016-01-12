package com.moji.zookeepernifty.example;


import org.apache.thrift.protocol.TProtocol;

import com.moji.zookeepernifty.ZkNiftyClientConfig;
import com.moji.zookeepernifty.example.HelloService;
import com.moji.zookeepernifty.syncclientpool.TProtocolPool;

public class PoolSyncClientExample {

	private static ZkNiftyClientConfig config;
	
	@SuppressWarnings("static-access")
	private static void runWithSingleThread() {
		final String path = "/HelloService/1.0.0";
		TProtocolPool pool = new TProtocolPool(config,path,HelloService.class);
		// 必须注册服务路径对应的Client类
		
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			System.out.print("Exception : " + e.getMessage());
		}
		
		long startTime = System.currentTimeMillis(); // 获取开始时间
		for(int i = 0; i < 100000;i++) {
			TProtocol protocol = pool.getTransport();
			try {
				HelloService.Client client = new HelloService.Client(protocol);
				if (protocol != null) {
					String echo = client.hello("World.");
					System.out.println(echo);
					echo = client.bye("World.");
					System.out.println(echo);
				}
			} catch (Exception e) {
				System.out.println("Talk Failed. Exception : " + e.getMessage());
			}
			pool.returnTransport(protocol);
		}
		
		long endTime=System.currentTimeMillis(); //获取结束时间  
	    System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
	    pool.close();
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
