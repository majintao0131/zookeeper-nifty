package com.moji.zookeepernifty;

import java.net.InetSocketAddress;
import java.util.List;

public interface ZkNiftyCallback {
	public void run(String service_path, List<InetSocketAddress> list);
}
