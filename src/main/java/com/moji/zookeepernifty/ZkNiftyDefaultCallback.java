package com.moji.zookeepernifty;

import java.net.InetSocketAddress;
import java.util.List;

public class ZkNiftyDefaultCallback implements ZkNiftyCallback {
	
	private ZkNiftyClientConfig _config;
	
	public ZkNiftyDefaultCallback(ZkNiftyClientConfig config) {
		this._config = config;
	}
	
	public void run(String service_path, List<InetSocketAddress> list) {
		ZkNiftyTransportManager manager = ZkNiftyTransportManager.getInstance(_config);
		manager.updateServiceAddressMap(service_path, list);
	}

}
