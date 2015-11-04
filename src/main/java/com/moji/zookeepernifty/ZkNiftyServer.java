package com.moji.zookeepernifty;

import org.apache.thrift.TProcessor;

public interface ZkNiftyServer {
	public void startServer(TProcessor processor) throws Exception;
}
