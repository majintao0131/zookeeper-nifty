package com.moji.zookeepernifty;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.nifty.client.NiftyClient;

import io.airlift.units.Duration;

public class DefaultZkNiftyClient extends AbstractZkNiftyClient {
	private static final Logger log = LoggerFactory.getLogger(DefaultZkNiftyClient.class);
	private static final Duration DEFAULT_CONNECT_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
	private static final Duration DEFAULT_RECEIVE_TIMEOUT = new Duration(10, TimeUnit.SECONDS);
	private static final Duration DEFAULT_READ_TIMEOUT = new Duration(5, TimeUnit.SECONDS);
	private static final Duration DEFAULT_SEND_TIMEOUT = new Duration(5, TimeUnit.SECONDS);
	private static final int DEFAULT_MAX_FRAME_SIZE = 16777216;
	
	public TBinaryProtocol createProtocol(Class<? extends TServiceClient> clientClass, NiftyClient client,
			InetSocketAddress address) throws TTransportException, InterruptedException {
	        FramedClientConnector framedClientConnector = new FramedClientConnector(address);
			try {
				TTransport transport = client.connectSync(clientClass, 
							framedClientConnector,
							DEFAULT_CONNECT_TIMEOUT,
							DEFAULT_RECEIVE_TIMEOUT, 
							DEFAULT_READ_TIMEOUT,
							DEFAULT_SEND_TIMEOUT,
							DEFAULT_MAX_FRAME_SIZE);
				
				TBinaryProtocol tp = new TBinaryProtocol(transport);
		        return tp;
			} catch (TTransportException e) {
				log.error("create Protocol Failed. Message : {}.", e.getMessage());
				e.printStackTrace();
				throw e;
			} catch (InterruptedException e) {
				log.error("create Protocol Failed. Message : {}.", e.getMessage());
				e.printStackTrace();
				throw e;
			}
	}
}
