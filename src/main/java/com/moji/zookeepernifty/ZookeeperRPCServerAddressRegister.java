package com.moji.zookeepernifty;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperRPCServerAddressRegister implements RPCServerAddressRegister {

	private static final Logger log = LoggerFactory.getLogger(ZookeeperRPCServerAddressRegister.class);
	private CuratorFramework _zkClient;
	private ZkNiftyServerConfig _config;
	private CuratorFrameworkState state;
	
	public ZookeeperRPCServerAddressRegister(ZkNiftyServerConfig config, CuratorFramework zkClient) {
		this._config = config;
		this._zkClient = zkClient;
	}
	
	public int register(String service, String version, String address) {
		try {
			if(_zkClient.getState() == CuratorFrameworkState.LATENT){
				_zkClient.start();
			}

			if (_zkClient.blockUntilConnected(3000, TimeUnit.MILLISECONDS)) {
				String path = _zkClient.create()
						.creatingParentsIfNeeded()
						.withMode(CreateMode.EPHEMERAL)
						.forPath("/" + service + "/" + version + "/" + address);
				log.debug("create path [{}] successful. " + path);
			} else {
				log.warn("CuratorFramework client connect failed.");
				return -1;
			}
		} catch (UnsupportedEncodingException e) {
			log.error("register service address to zookeeper exception:{}",e.getMessage());
			e.printStackTrace();
			return -1;
		} catch (InterruptedException e) {
			log.warn("CuratorFramework Client interrupted. Exception message [{}].", e.getMessage());
			e.printStackTrace();
			return -1;
		} catch (Exception e) {
			log.error("register service address to zookeeper exception:{}",e.getMessage());
			e.printStackTrace();
			return -1;
		}
		
		return 0;
	}
	
	public int unregister(String service, String version, String address) {
		try {
			if(_zkClient.getState() == CuratorFrameworkState.LATENT){
				_zkClient.start();
			}
			
			if (_zkClient.blockUntilConnected(3000, TimeUnit.MILLISECONDS)) {
				_zkClient.delete().forPath("/" + service + "/" + version + "/" + address);
				log.debug("delete path [{}] successful. " + address);
			}
			
		} catch (InterruptedException e) {
			log.warn("CuratorFramework Client interrupted. Exception message [{}].", e.getMessage());
			e.printStackTrace();
			return -1;
		} catch (Exception e) {
			log.error("delete path[{}] failed. Message : {}.", service + "/" + version, e.getMessage());
			e.printStackTrace();
			return -1;
		}
		
		return 0;
	}
	
	public void close() throws IOException {
		_zkClient.close();
	}
	
}
