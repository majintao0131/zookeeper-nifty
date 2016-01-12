package com.moji.zookeepernifty.syncclientpool;

import java.net.InetSocketAddress;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facebook.nifty.client.NiftyClient;
import com.moji.zookeepernifty.DefaultZkNiftyClient;



public class TProtocolPoolFactory extends BasePooledObjectFactory<TProtocol> {
		
	private static Logger logger = LoggerFactory.getLogger(TProtocolPoolFactory.class);
	@SuppressWarnings("rawtypes")
	private final Class clazz;
	private final NiftyClient niftyClient;
	private final TProtocolPool tprotocolPool;
	private static DefaultZkNiftyClient zkNiftyClient = new DefaultZkNiftyClient();
	public TProtocolPoolFactory(@SuppressWarnings("rawtypes") Class clazz, NiftyClient niftyClient, TProtocolPool tprotocolPool) {
		super();
		this.clazz = clazz;
		this.niftyClient = niftyClient;
		this.tprotocolPool = tprotocolPool;
	}

	@SuppressWarnings("unchecked")
	@Override
	public TProtocol create() throws Exception {
		InetSocketAddress address = tprotocolPool.getRandomHost();
		if(address == null) 
			return null;
		return zkNiftyClient.createProtocol(clazz, niftyClient, address);
	}

	@Override
	public PooledObject<TProtocol> wrap(TProtocol obj) {
		return new DefaultPooledObject<TProtocol>(obj);
	}

	@Override
	public void destroyObject(PooledObject<TProtocol> protocal) throws Exception {
		logger.info("destroyObject:{}", protocal);
		protocal.getObject().getTransport().close();
	}

	@Override
	public boolean validateObject(PooledObject<TProtocol> p) {
		return p.getObject().getTransport().isOpen();
	}
	
}
