package com.moji.zookeepernifty;

import java.io.IOException;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import com.moji.zookeepernifty.RPCServerAddressProvider;

public class ZookeeperRPCServerAddressProvider implements RPCServerAddressProvider, Closeable {

	private String service_name;
	private String service_version;
	private CuratorFramework zkClient;
	private PathChildrenCache cachedPath;
	private ZkNiftyTransportManager manager;
	private ZkNiftyCallback _callback;
	private ZkNiftyClientConfig _config;
	
	private List<InetSocketAddress> address_list = new ArrayList<InetSocketAddress>();
	
	//同步器
	private CountDownLatch countDownLatch= new CountDownLatch(1);
	
	// 默认权重
	private static final Integer DEFAULT_WEIGHT = 1;
	
	public ZookeeperRPCServerAddressProvider(ZkNiftyClientConfig config) {
		this._config = config;
	}
	
	public void close() throws IOException {
		try {
			cachedPath.clear();
			cachedPath.close();
			zkClient.close();
		} catch (Exception e) {
			
		}
	}
	
	public int reset(String path, List<InetSocketAddress> list) {
		// 执行回调函数，更新client列表
		_callback.run(path, list);
		return 0;
	}

	public void Init() throws Exception {
		try {
			CuratorFrameworkFactory.Builder builder=CuratorFrameworkFactory.builder();
			zkClient = builder.connectString(_config.getZookeeper())
					.sessionTimeoutMs(30000)
					.connectionTimeoutMs(30000)
					.retryPolicy(new ExponentialBackoffRetry(1000,3)).build();
			if (zkClient.getState() == CuratorFrameworkState.LATENT) {
				zkClient.start();
			}
			
			buildPathChildrenCache(zkClient, getServicePath(), true);
			if (cachedPath == null) {
				System.out.println("cachedPath is null.");
				return;
			}
			cachedPath.start(StartMode.POST_INITIALIZED_EVENT);
			countDownLatch.await();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw e;
		}
	}

	private String getServicePath() {
		return "/" + this.service_name + "/" + this.service_version;
	}
	
	private List<InetSocketAddress> transfer(String address) {
		String[] hostname = address.split(":");
		Integer weight = DEFAULT_WEIGHT;
		if (hostname.length == 3) {
			weight = Integer.valueOf(hostname[2]);
		}
		
		String ip = hostname[0];
		Integer port = Integer.valueOf(hostname[1]);
		
		for (int i = 0; i < weight; ++i) {
			address_list.add(new InetSocketAddress(ip, port));
		}
		
		return address_list;
	}
	
	private void buildPathChildrenCache(final CuratorFramework client, final String path, Boolean bCache) throws Exception {
		cachedPath = new PathChildrenCache(client, path, bCache);
		cachedPath.getListenable().addListener(new PathChildrenCacheListener() {
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
				PathChildrenCacheEvent.Type eventType = event.getType();
				switch (eventType) {
					case CONNECTION_RECONNECTED:
						System.out.println("Connection is reconnection.");
						break;
					case CONNECTION_SUSPENDED:
						System.out.println("Connection is suspended.");
						break;
					case CONNECTION_LOST:
						System.out.println("Connection error, waiting...");
						return;
					case INITIALIZED:
						System.out.println("Connection init...");
					default:
						break;
				}
				cachedPath.rebuild();
				rebuild();
				if (countDownLatch.getCount() > 0) {
					countDownLatch.countDown();
				}
			}
			
			private void rebuild() throws Exception {
				List<ChildData> children = cachedPath.getCurrentData();
				if (children == null || children.isEmpty()) {
					System.out.println("rpc server-cluster error...");
					return;
				}
				
				String host_path = null;
				for (ChildData data : children) {
					host_path = data.getPath();
					host_path = host_path.substring(getServicePath().length() + 1);
					String address = new String(host_path.getBytes(), "utf-8");
					reset(path, transfer(address));
				}
			}
		});
	}

	public void registerCallback(ZkNiftyCallback callback) {
		this._callback = callback;
	}

}
