package com.moji.zookeepernifty.syncclientpool;

import java.net.InetSocketAddress;
import java.util.List;

import com.moji.zookeepernifty.ZkNiftyCallback;

public class TProtocolPoolCallback implements ZkNiftyCallback {
	
	private TProtocolPool tProtocolPool;
	
	public TProtocolPoolCallback(TProtocolPool tProtocolPool) {
		super();
		this.tProtocolPool = tProtocolPool;
	}

	@Override
	public void run(String service_path, List<InetSocketAddress> list) {
		if(list == null || list.size() == 0) {
			tProtocolPool.clearServicePool(service_path);
		} else {
			tProtocolPool.updateAddressList(list);			
		}
	}
}
