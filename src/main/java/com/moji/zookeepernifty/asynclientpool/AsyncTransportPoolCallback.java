package com.moji.zookeepernifty.asynclientpool;

import java.net.InetSocketAddress;
import java.util.List;

import com.moji.zookeepernifty.ZkNiftyCallback;

public class AsyncTransportPoolCallback implements ZkNiftyCallback {
	
	@Override
	public void run(String service_path, List<InetSocketAddress> list) {
		AsyncTransportPool.updateAddressList(list);
	}
}
