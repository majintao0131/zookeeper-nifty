package com.moji.zookeepernifty;

import java.io.IOException;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.ArrayList;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import com.moji.zookeepernifty.RPCServerAddressProvider;

public class ZookeeperRPCMutilServerAddressProvider implements RPCServerAddressProvider, Closeable {
	
	private static final Logger log = LoggerFactory.getLogger(ZookeeperRPCMutilServerAddressProvider.class);
	private static final int DEFAULT_WEIGHT = 1;
	
	private CuratorFramework _zkClient = null;
	private ArrayList<PathChildrenCache> _cachedPath_list = null;
	private ZkNiftyCallback _callback = null;
	private ZkNiftyClientConfig _config;
	private Boolean _bContinue = true;
	
	public ZookeeperRPCMutilServerAddressProvider(ZkNiftyClientConfig config) {
		this._config = config;
	}
	
	public void close() throws IOException {
		try {
			_bContinue = false;
			if (_cachedPath_list != null) {
				for (PathChildrenCache pathChildrenCache : _cachedPath_list) {
					pathChildrenCache.clear();
					pathChildrenCache.close();
				}
				_cachedPath_list.clear();
			}
			if (_zkClient != null) {
				_zkClient.close();
			}
		} catch (Exception e) {
			log.warn("close has Exception[{}].", e.getMessage());
		}
	}
	
	public int reset(String path, List<InetSocketAddress> list) {
		if (_callback == null) {
			log.warn("No callback has been register for service[{}].", path);
			return -1;
		}
		// 执行回调函数，更新client列表
		_callback.run(path, list);
		return 0;
	}
	
	public void init() throws Exception {
		try {
			if (_cachedPath_list == null) {
				_cachedPath_list = new ArrayList<PathChildrenCache>();
				_cachedPath_list.clear();
			}
			CuratorFrameworkFactory.Builder builder=CuratorFrameworkFactory.builder();
			log.debug("Zookeeper address : {}.", _config.getZookeeper());
			_zkClient = builder.connectString(_config.getZookeeper())
					.sessionTimeoutMs(30000)
					.connectionTimeoutMs(30000)
					.retryPolicy(new ExponentialBackoffRetry(1000,3)).build();
			if (_zkClient.getState() == CuratorFrameworkState.LATENT) {
				_zkClient.start();
			}
			
			List<String> service_list = _config.getServicePathList();
			for (String path : service_list) {
				PathChildrenCache cachedPath = buildPathChildrenCache(_zkClient, path, true);
				if (cachedPath == null) {
					System.out.println("cachedPath is null.");
					return;
				}
				cachedPath.start(StartMode.POST_INITIALIZED_EVENT);
			}
		} catch (Exception e) {
			log.error("buildPathChildrenCache failed. Error message[{}].", e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void quit() {
		_bContinue = false;
	}
	
	private List<InetSocketAddress> transfer(String address) {
		String[] hostname = address.split(":");
		Integer weight = DEFAULT_WEIGHT;
		if (hostname.length == 3) {
			weight = Integer.valueOf(hostname[2]);
		}
		
		String ip = hostname[0];
		Integer port = Integer.valueOf(hostname[1]);
		ArrayList<InetSocketAddress> address_list = new ArrayList<InetSocketAddress>();
		for (int i = 0; i < weight; ++i) {
			address_list.add(new InetSocketAddress(ip, port));
		}
		
		return address_list;
	}
	
	private PathChildrenCache buildPathChildrenCache(final CuratorFramework client, final String path, Boolean bCache) throws Exception {
		final PathChildrenCache cachedPath = new PathChildrenCache(client, path, bCache);
		_cachedPath_list.add(cachedPath);
		cachedPath.getListenable().addListener(new PathChildrenCacheListener() {
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
				PathChildrenCacheEvent.Type eventType = event.getType();
				switch (eventType) {
					case CHILD_UPDATED:
						System.out.println("Update : " + cachedPath.getCurrentData());
						break;
					case CHILD_ADDED:
						System.out.println("Add : " + cachedPath.getCurrentData());
						break;
					case CHILD_REMOVED:
						System.out.println("Remove : " + cachedPath.getCurrentData());
						break;
					case CONNECTION_RECONNECTED:
						log.debug("Connection[{}] is reconnection.", path);
						break;
					case CONNECTION_SUSPENDED:
						log.debug("Connection[{}] is suspended.", path);
						break;
					case CONNECTION_LOST:
						log.debug("Connection[{}] error, waiting...", path);
						break;
					case INITIALIZED:
						log.debug("Connection[{}] init...", path);
						break;
					default:
						break;
				}
				log.debug("PathChildrenCacheEvent[{}] for path[{}] has a event.", eventType, path);
				cachedPath.rebuild();
				rebuild();
			}
			
			private void rebuild() throws Exception {
				List<ChildData> children = cachedPath.getCurrentData();
				if (children == null || children.isEmpty()) {
					System.out.println("rpc server-cluster error...");
					return;
				}
				
				String path_address = null;
				List<InetSocketAddress> address_list = new ArrayList<InetSocketAddress>();
				
				for (ChildData data : children) {
					path_address = data.getPath();
					path_address = path_address.substring(path_address.lastIndexOf('/') + 1);
					String address = new String(path_address.getBytes(), "utf-8");
					// 触发地址更新操作
					address_list.addAll(transfer(address));
				}
				
				reset(path, address_list);
			}
		});
		return cachedPath;
	}

	public void registerCallback(ZkNiftyCallback callback) {
		this._callback = callback;
	}

}
