package com.moji.zookeepernifty.syncclientpool;

import java.net.InetSocketAddress;
import java.util.List;

import com.moji.zookeepernifty.ZkNiftyCallback;

public class TProtocolPoolCallback implements ZkNiftyCallback {
	
	@Override
	public void run(String service_path, List<InetSocketAddress> list) {
		if(list == null || list.size() == 0) {
			TProtocolPool.clearServicePool(service_path);
		} else {
			TProtocolPool.updateAddressList(list);			
		}
	}
}
