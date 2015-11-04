package com.moji.zookeepernifty;

import java.io.Closeable;
import java.net.InetSocketAddress;

import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;

import com.facebook.nifty.client.NiftyClient;

/**
 * clients use zookeeper to find services
 * @author jintao.ma
 *
 */

public interface ZkNiftyClient extends Closeable {
	
	/**
	 * 当zookeeper列表发生变化时，会执行注册的callback进行client列表更新
	 * @param callback
	 */
	public TBinaryProtocol createProtocol(Class<? extends TServiceClient> clientClass, 
			NiftyClient client, InetSocketAddress address)  throws TTransportException, InterruptedException;
}
