package com.moji.zookeepernifty;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;

import com.facebook.nifty.client.NiftyClient;

public class AbstractZkNiftyClient implements ZkNiftyClient {

	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

	public TBinaryProtocol createProtocol(Class<? extends TServiceClient> clientClass, NiftyClient client,
			InetSocketAddress address) throws TTransportException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}
	

}
