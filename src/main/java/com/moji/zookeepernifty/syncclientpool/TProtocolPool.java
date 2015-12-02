package com.moji.zookeepernifty.syncclientpool;

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
import org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facebook.nifty.client.NettyClientConfigBuilder;
import com.facebook.nifty.client.NiftyClient;
import com.moji.zookeepernifty.ZkNiftyClientConfig;
import com.moji.zookeepernifty.ZookeeperRPCMutilServerAddressProvider;

public class TProtocolPool {
	
	private static Logger logger = LoggerFactory.getLogger(TProtocolPool.class);
	private static TProtocolPool instance = null;
	private static NettyClientConfigBuilder nettyClientConfigBuilder = null;
	private static NiftyClient niftyClient = null;
	private static ZkNiftyClientConfig config;
	private static List<InetSocketAddress> containerList = new ArrayList<InetSocketAddress>();
	private static Queue<InetSocketAddress> innerQueue = new LinkedList<InetSocketAddress>();
	private static ZookeeperRPCMutilServerAddressProvider provider = null;
	private static Map<String, GenericObjectPool<TProtocol>> poolMap = new HashMap<String, GenericObjectPool<TProtocol>>();
	private static Object lock = new Object();
	private TProtocolPool(ZkNiftyClientConfig config) {
		TProtocolPool.config = config;
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
		provider.registerCallback(new TProtocolPoolCallback());
		try {
			provider.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static TProtocolPool getInstance(ZkNiftyClientConfig config) {
		if(instance == null) {
			instance = new TProtocolPool(config);
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
	
	private static InetSocketAddress getRandomHost() {
		if (innerQueue.isEmpty()) {
			if (!containerList.isEmpty()) {
				Collections.shuffle(containerList);
				innerQueue.addAll(containerList);
			}
		}
		return innerQueue.poll();
	}
	
	public static TProtocol getTransport(String path, Class<?> clazz) {
		GenericObjectPool<TProtocol> pool = poolMap.get(path);
		if(pool == null) {
			GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
			poolConfig.setMaxTotal(config.getTransportCount(path));
			poolConfig.setBlockWhenExhausted(true);
			poolConfig.setMaxWaitMillis(-1); //获取不到永远等待
			InetSocketAddress address = getRandomHost();
			TProtocolPoolFactory protocolPoolFactory = new TProtocolPoolFactory(clazz, address, niftyClient);
			GenericObjectPool<TProtocol> poolInit = new GenericObjectPool<TProtocol>(protocolPoolFactory,poolConfig);
			poolMap.put(path, poolInit);
			pool = poolInit;
		}
		try {
			return pool.borrowObject();
		} catch (Exception e) {
			logger.error("get transport form pool failed. error message [{}]",e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	public static void returnTransport(String path, TProtocol obj) {
		GenericObjectPool<TProtocol> pool = poolMap.get(path);
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
}
