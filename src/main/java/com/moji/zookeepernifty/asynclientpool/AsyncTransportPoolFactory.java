package com.moji.zookeepernifty.asynclientpool;

import java.net.InetSocketAddress;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AsyncTransportPoolFactory extends BasePooledObjectFactory<TNonblockingTransport> {
		
	private static Logger logger = LoggerFactory.getLogger(AsyncTransportPoolFactory.class);

	private final InetSocketAddress address;
	private final int clientTimeout; 
	public AsyncTransportPoolFactory(int clientTimeout,
			InetSocketAddress address) {
		super();
		this.clientTimeout = clientTimeout;
		this.address = address;
	}

	@Override
	public TNonblockingTransport create() throws Exception {
		TNonblockingSocket transport = new TNonblockingSocket(address.getHostString(), address.getPort(), clientTimeout); 
		return transport;
	}

	@Override
	public PooledObject<TNonblockingTransport> wrap(TNonblockingTransport obj) {
		return new DefaultPooledObject<TNonblockingTransport>(obj);
	}

	@Override
	public void destroyObject(PooledObject<TNonblockingTransport> transport) throws Exception {
		logger.info("destroyObject:{}", transport);
		transport.getObject().close();
	}

	@Override
	public boolean validateObject(PooledObject<TNonblockingTransport> t) {
		return t.getObject().isOpen();
	}
	
}
