package com.moji.zookeepernifty;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.List;

public interface RPCServerAddressProvider extends Closeable {	
	/**
	 * 初始化provider内部的服务地址列表
	 * @param list
	 * @return
	 */
	public int reset(String path, List<InetSocketAddress> list);
	
	/**
	 * 注册一个回调函数，当监控节点变化时进行回调
	 */
	public void registerCallback(ZkNiftyCallback callback);
}
