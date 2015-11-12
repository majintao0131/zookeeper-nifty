package com.moji.zookeepernifty;

import org.apache.thrift.TProcessor;

public interface ZkNiftyServer {
	public int init();
	public void startServer(TProcessor processor) throws Exception;
}
