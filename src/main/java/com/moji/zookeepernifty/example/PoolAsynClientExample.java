package com.moji.zookeepernifty.example;


import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TNonblockingTransport;

import com.moji.zookeepernifty.ZkNiftyClientConfig;
import com.moji.zookeepernifty.asynclientpool.AsyncTransportPool;
import com.moji.zookeepernifty.example.HelloService;

public class PoolAsynClientExample {

	private static ZkNiftyClientConfig config;
	
	@SuppressWarnings("static-access")
	private static void runWithSingleThread() {
		AsyncTransportPool pool = AsyncTransportPool.getInstance(config);

		final String path = "/HelloService/1.0.0";
		// 必须注册服务路径对应的Client类
		
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			System.out.print("Exception : " + e.getMessage());
		}
		
		long startTime = System.currentTimeMillis(); // 获取开始时间
		TAsyncClientManager clientManager = null;
		
		try {
			clientManager = new TAsyncClientManager();
		}catch(Exception e) {
		}

		for(int i = 0; i < 10;i++) {
			TProtocolFactory protocol = new TBinaryProtocol.Factory();
			TNonblockingTransport transport = pool.getTransport(path);
			HelloService.AsyncClient asyncClient = new HelloService.AsyncClient(protocol, clientManager, transport);
			try {
				if (asyncClient != null) {
					asyncClient.hello("World.", new MyCallback(pool,path, transport));
				}
			} catch (Exception e) {
				System.out.println("Talk Failed. Exception : " + e.getMessage());
			}
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
