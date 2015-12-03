package com.moji.zookeepernifty;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.thrift.TProcessor;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facebook.nifty.core.NettyServerConfig;
import com.facebook.nifty.core.NettyServerConfigBuilder;
import com.facebook.nifty.core.NettyServerTransport;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.moji.zookeepernifty.AbstractZkNiftyServer;
import com.moji.zookeepernifty.ZkNiftyServerConfig;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class DefaultZkNiftyServer extends AbstractZkNiftyServer {

	private static final Logger log = LoggerFactory.getLogger(DefaultZkNiftyServer.class);
	
	private ThriftServerDefBuilder _thriftServerDefBuilder;
	private NettyServerTransport _server = null;
	private ZkNiftyServerConfig _config;
	private ZookeeperRPCServerAddressRegister _register = null;
	private CuratorFramework _zkClient = null;
	
	public DefaultZkNiftyServer(ZkNiftyServerConfig config) {
		this._config  = config;
	}
	
	public int init() {
		if (_zkClient == null) {
			try {
				_zkClient = CuratorFrameworkFactory
						.builder()
						.connectString(_config.getZookeeper())
						.sessionTimeoutMs(30000)
						.connectionTimeoutMs(30000)
						.retryPolicy(new ExponentialBackoffRetry(1000,3))
						.build();
			} catch (Exception e) {
				e.printStackTrace();
				log.error("Build Curator client failed.");
				return -1;
			}
		}
		
		if (_register == null) {
			_register = new ZookeeperRPCServerAddressRegister(_config, _zkClient);
		}
		
		return 0;
	}
	
	public void start(TProcessor processor) throws Exception {
		_thriftServerDefBuilder = new ThriftServerDefBuilder()
					.listen(_config.getListenPort())
					.withProcessor(processor);
		try {
			_server = new NettyServerTransport(_thriftServerDefBuilder.build(),
					defaultThriftServerConfigBuilder().build(),
					new DefaultChannelGroup());
			_server.start();
			if (registerService() < 0) {
				throw new Exception("Register service failed.");
			}
		} catch (Exception e) {
			log.error("start server failed. [{}]", e.getMessage());
			e.printStackTrace();
			unregisterService();
			_server.stop();
			throw e;
		}
	}
	
	private NettyServerConfigBuilder defaultThriftServerConfigBuilder() throws Exception {
		try {
			NettyServerConfigBuilder configBuilder = NettyServerConfig.newBuilder();
			configBuilder.setBossThreadCount(_config.getBossThreadCount());
			configBuilder.setWorkerThreadCount(_config.getWorkerThreadCount());
			ExecutorService boss = newFixedThreadPool(_config.getBossThreadCount());
			ExecutorService workers = newFixedThreadPool(_config.getWorkerThreadCount());
			configBuilder.setBossThreadExecutor(boss);
			configBuilder.setWorkerThreadExecutor(workers);
			
			return configBuilder;
		} catch (Exception e) {
			throw e;
		}
	}
	
	public void close() {
		if (unregisterService() < 0) {
			log.warn("unregister service failed.");
		}
		try {
			_register.close();
		} catch (IOException e) {
			e.printStackTrace();
			log.error("RPCServer Register quit failed. Exception Message : [{}].", e.getMessage());
		}
		
		if (_server != null) {
			try {
				_server.stop();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error("Server stop failed.");
			}
		}
	}

	private int registerService() {
		String address = CommonUtil.getLocalHostIP();
		if (address == null) {
			log.error("Cannt get localhost address.");
			return -1;
		}
		
		address = address + ":" + _config.getListenPort();
		if (_register.register(_config.getServiceName(), _config.getServiceVersion(), address) < 0) {
			log.warn("The path [{}] Register to zookeeper failed.", "/" + _config.getServiceName() + "/" + _config.getServiceVersion() + "/");
			return -1;
		}
		
		return 0;
	}
	
	private int unregisterService() {
		String address = CommonUtil.getLocalHostIP();
		if (address == null) {
			log.error("Cannt get localhost address.");
			return -1;
		}
		
		address = address + ":" + _config.getListenPort();
		if (_register.unregister(_config.getServiceName(), _config.getServiceVersion(), address) < 0) {
			log.warn("The path [{}] unregister to zookeeper failed, Host[{}].", "/" + _config.getServiceName() + "/" + _config.getServiceVersion() + "/", address);
			return -1;
		}
		return 0;
	}
}
