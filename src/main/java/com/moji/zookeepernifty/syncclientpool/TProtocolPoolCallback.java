package com.moji.zookeepernifty.syncclientpool;

import java.net.InetSocketAddress;
import java.util.List;

import com.moji.zookeepernifty.ZkNiftyCallback;

public class TProtocolPoolCallback implements ZkNiftyCallback {
	
	@Override
	public void run(String service_path, List<InetSocketAddress> list) {
		TProtocolPool.updateAddressList(list);
	}
}
