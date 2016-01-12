package com.moji.zookeepernifty.asynclientpool;

import java.net.InetSocketAddress;
import java.util.List;

import com.moji.zookeepernifty.ZkNiftyCallback;

public class AsyncTransportPoolCallback implements ZkNiftyCallback {
	
	private AsyncTransportPool asyncTransportPool;
	
	public AsyncTransportPoolCallback(AsyncTransportPool asyncTransportPool) {
		this.asyncTransportPool = asyncTransportPool;
	}

	@Override
	public void run(String service_path, List<InetSocketAddress> list) {
		if(list == null || list.size() == 0) {
			asyncTransportPool.clearServicePool(service_path);
		} else {
			asyncTransportPool.updateAddressList(list);
		}
	}
}
