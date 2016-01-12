package com.moji.zookeepernifty.asynclientpool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.transport.TNonblockingTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facebook.nifty.client.NettyClientConfigBuilder;
import com.facebook.nifty.client.NiftyClient;
import com.moji.zookeepernifty.ZkNiftyClientConfig;
import com.moji.zookeepernifty.ZookeeperRPCMutilServerAddressProvider;

public class AsyncTransportPool {
	
	private static Logger logger = LoggerFactory.getLogger(AsyncTransportPool.class);
	private NettyClientConfigBuilder nettyClientConfigBuilder = null;
	private NiftyClient niftyClient = null;
	private ZkNiftyClientConfig config;
	private List<InetSocketAddress> containerList = new ArrayList<InetSocketAddress>();
	private Queue<InetSocketAddress> innerQueue = new LinkedList<InetSocketAddress>();
	private ZookeeperRPCMutilServerAddressProvider provider = null;
	private GenericObjectPool<TNonblockingTransport> pool = null;
	private Object lock = new Object();
	private final int DEFAULT_CLIENT_TIMEOUT = 30000;
	public AsyncTransportPool(ZkNiftyClientConfig config, String path) {
		this.config = config;
		if (nettyClientConfigBuilder == null) {
			nettyClientConfigBuilder = new NettyClientConfigBuilder();
		}
		nettyClientConfigBuilder.setBossThreadCount(config.getBossThreadCount());
		nettyClientConfigBuilder.setWorkerThreadCount(config.getWorkerThreadCount());
		nettyClientConfigBuilder.setBossThreadExecutor(Executors.newFixedThreadPool(config.getBossThreadCount()));
		nettyClientConfigBuilder.setWorkerThreadExecutor(Executors.newFixedThreadPool(config.getWorkerThreadCount()));
		
		if (niftyClient == null) {
			niftyClient = new NiftyClient(nettyClientConfigBuilder.build());
		}
		
		if (provider == null) {
			provider = new ZookeeperRPCMutilServerAddressProvider(config);
		}
		provider.registerCallback(new AsyncTransportPoolCallback(this));
		try {
			provider.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		poolConfig.setMaxTotal(config.getTransportPoolCount(path).getMaxTotal());
		poolConfig.setMaxIdle(config.getTransportPoolCount(path).getMaxIdle());
		poolConfig.setMinIdle(config.getTransportPoolCount(path).getMinIdle());
		poolConfig.setBlockWhenExhausted(true);
		poolConfig.setMaxWaitMillis(-1); //获取不到永远等待
		AsyncTransportPoolFactory transportPoolFactory = new AsyncTransportPoolFactory(DEFAULT_CLIENT_TIMEOUT,this);
		GenericObjectPool<TNonblockingTransport> poolInit = new GenericObjectPool<TNonblockingTransport>(transportPoolFactory,poolConfig);
		pool = poolInit;
	}
	
	public void updateAddressList(List<InetSocketAddress> container) {
		synchronized (lock) {
			containerList.clear();
			containerList.addAll(container);
			innerQueue.clear();
			innerQueue.addAll(container);
		}
	}
	
	InetSocketAddress getRandomHost() {
		if (innerQueue.isEmpty()) {
			if (!containerList.isEmpty()) {
				Collections.shuffle(containerList);
				innerQueue.addAll(containerList);
			}
		}
		return innerQueue.poll();
	}
	
	public TNonblockingTransport getTransport() {
		try{
			return pool.borrowObject();
		} catch (Exception e) {
			logger.error("get transport form pool failed. error message [{}]",e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	public void returnTransport(TNonblockingTransport obj) {
		if(pool !=null && obj != null && obj.isOpen())
			pool.returnObject(obj);
	}
	
	public void close() {
		try {
			niftyClient.close();
			provider.close();
		} catch (IOException e) {
			logger.error("close Transport pool failed. error message [{}]",e.getMessage());
			e.printStackTrace();
		}
		logger.info("pool closed");
	}
	
	public void clearServicePool(String path) {
		synchronized (lock) {
			innerQueue.clear();
			containerList.clear();
		}
	}
}
