package com.moji.zookeepernifty.asynclientpool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
	private static AsyncTransportPool instance = null;
	private static NettyClientConfigBuilder nettyClientConfigBuilder = null;
	private static NiftyClient niftyClient = null;
	private static ZkNiftyClientConfig config;
	private static List<InetSocketAddress> containerList = new ArrayList<InetSocketAddress>();
	private static Queue<InetSocketAddress> innerQueue = new LinkedList<InetSocketAddress>();
	private static ZookeeperRPCMutilServerAddressProvider provider = null;
	private static Map<String, GenericObjectPool<TNonblockingTransport>> poolMap = new HashMap<String, GenericObjectPool<TNonblockingTransport>>();
	private static Object lock = new Object();
	private static final int DEFAULT_CLIENT_TIMEOUT = 30000;
	private AsyncTransportPool(ZkNiftyClientConfig config) {
		AsyncTransportPool.config = config;
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
		provider.registerCallback(new AsyncTransportPoolCallback());
		try {
			provider.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static AsyncTransportPool getInstance(ZkNiftyClientConfig config) {
		if(instance == null) {
			instance = new AsyncTransportPool(config);
		}
		return instance;
	}
	
	public static void updateAddressList(List<InetSocketAddress> container) {
		synchronized (lock) {
			containerList.clear();
			containerList.addAll(container);
			innerQueue.clear();
			innerQueue.addAll(container);
		}
	}
	
	static InetSocketAddress getRandomHost() {
		if (innerQueue.isEmpty()) {
			if (!containerList.isEmpty()) {
				Collections.shuffle(containerList);
				innerQueue.addAll(containerList);
			}
		}
		return innerQueue.poll();
	}
	
	public static TNonblockingTransport getTransport(String path) {
		GenericObjectPool<TNonblockingTransport> pool = poolMap.get(path);
		try {
			if(pool == null) {
				if(containerList.size() == 0) {
					return null;
				}
				GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
				poolConfig.setMaxTotal(config.getTransportPoolCount(path).getMaxTotal());
				poolConfig.setMaxIdle(config.getTransportPoolCount(path).getMaxIdle());
				poolConfig.setMinIdle(config.getTransportPoolCount(path).getMinIdle());
				poolConfig.setBlockWhenExhausted(true);
				poolConfig.setMaxWaitMillis(-1); //获取不到永远等待
				AsyncTransportPoolFactory transportPoolFactory = new AsyncTransportPoolFactory(DEFAULT_CLIENT_TIMEOUT);
				GenericObjectPool<TNonblockingTransport> poolInit = new GenericObjectPool<TNonblockingTransport>(transportPoolFactory,poolConfig);
				poolMap.put(path, poolInit);
				pool = poolInit;
			}
			return pool.borrowObject();
		} catch (Exception e) {
			logger.error("get transport form pool failed. error message [{}]",e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	public static void returnTransport(String path, TNonblockingTransport obj) {
		GenericObjectPool<TNonblockingTransport> pool = poolMap.get(path);
		if(pool !=null && obj != null && obj.isOpen())
			pool.returnObject(obj);
	}
	
	public static void close() {
		try {
			niftyClient.close();
			provider.close();
		} catch (IOException e) {
			logger.error("close Transport pool failed. error message [{}]",e.getMessage());
			e.printStackTrace();
		}
		logger.info("pool closed");
	}
	
	public static void clearServicePool(String path) {
		synchronized (lock) {
			innerQueue.clear();
			containerList.clear();
			 GenericObjectPool<TNonblockingTransport> pool = poolMap.get(path);
			if(pool != null) {
				pool.close();
				poolMap.remove(path);
			}
		}
	}
}
