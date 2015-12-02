package com.moji.zookeepernifty.example;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.transport.TNonblockingTransport;

import com.moji.zookeepernifty.asynclientpool.AsyncTransportPool;


/**
 * @author 吕桂强
 * @email larry.lv.word@gmail.com
 * @version 创建时间：2012-4-25 上午11:17:32
 */
public class MyCallback implements AsyncMethodCallback<HelloService.AsyncClient.hello_call> {

	private AsyncTransportPool pool = null;
	private String path;
	private TNonblockingTransport transport;
	public MyCallback(AsyncTransportPool pool, String path, TNonblockingTransport transport) {
		this.pool = pool;
		this.path = path;
		this.transport = transport;
	}
	// 返回结果
	@SuppressWarnings("static-access")
	@Override
	public void onComplete(HelloService.AsyncClient.hello_call response) {
		System.out.println("onComplete");
		try {
			System.out.println(response.getResult().toString());
			pool.returnTransport(path, transport);
		} catch (TException e) {
			e.printStackTrace();
		}
	}

	// 返回异常
	@Override
	public void onError(Exception exception) {
		exception.printStackTrace();
		System.out.println("onError");
	}

}