package com.moji.zookeepernifty;


import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facebook.nifty.client.NettyClientConfigBuilder;
import com.facebook.nifty.client.NiftyClient;

import junit.extensions.ExceptionTestCase;

public class ZkNiftyTransportManager {
	
	private static final Logger log = LoggerFactory.getLogger(ZkNiftyTransportManager.class);
	private static final int UPDATE_HOSTVERSION_SUCCESS = 0;
	private static final int CREATE_NEW_HOSTVERSION = 1;
	
	public class HostVersion {
		private InetSocketAddress address;
		private int version;
		
		public HostVersion(InetSocketAddress address, int version) {
			this.version = version;
			this.address = address;
		}
		
		public InetSocketAddress getAddress() {
			return this.address;
		}
		
		public int getVersion() {
			return this.version;
		}
	}
	public class ServiceVersion {
		private Lock lock = null;
		private String service_path;
		private int version;
		private int offset;
		private int count;
		private ArrayList<HostVersion> host_list;
		
		public ServiceVersion(String path) {
			this.service_path = path;
			this.version = 1;
			this.offset = 0;
			this.count = 0;
			this.host_list = new ArrayList<ZkNiftyTransportManager.HostVersion>();
			this.host_list.clear();
			if (lock == null) {
				lock = new ReentrantLock();
			}
		}
		
		// 获取一个可用的hostVersion
		public HostVersion getOneHostVersion() {
			lock.lock();
			int bgn_index = offset;
			if (bgn_index > count) {
				bgn_index = 0;
			}
			
			HostVersion hostVersion = host_list.get(bgn_index);
			int index = bgn_index + 1;
			while (hostVersion.version != version && index != bgn_index) {
				hostVersion = host_list.get(index);
				index = (index + 1) % count;
			}
			lock.unlock();
			if (hostVersion.version != version) {
				log.warn("No available host for service[{}].", service_path);
				return null;
			}
			
			return hostVersion;
		}
		
		// 更新ServiceVersoin的version
		public ServiceVersion incServiceVersion() {
			++this.version;
			return this;
		}
		
		public int getServiceVersion() {
			return this.version;
		}
		
		// 添加一个hostVersion
		public ServiceVersion addHostVersion(HostVersion hostVersion) {
			lock.lock();
			this.host_list.add(hostVersion);
			++this.count;
			lock.unlock();
			log.debug("Add a new HostVersion which address is [{}] into ServiceVersion.", hostVersion.address);
			return this;
		}
		
		public int updateHostVersion(InetSocketAddress address) {
			lock.lock();
			for (HostVersion hostVersion : host_list) {
				if (hostVersion.address.equals(address)) {
					hostVersion.version = this.version;
					log.debug("Update the HostVersion which address is [{}] to Version[{}].", address, hostVersion.version);
					return UPDATE_HOSTVERSION_SUCCESS;
				}
			}
			
			// 如果是新增的host，则添加到list中
			HostVersion hostVersion = new HostVersion(address, this.version);
			this.host_list.add(hostVersion);
			lock.unlock();
			return CREATE_NEW_HOSTVERSION;
		}
		
		// 清理ServiceVersion内部过期的hostVersion
		public ServiceVersion cleanServiceVersion() {
			lock.lock();
			for (int i = 0; i < count && i < host_list.size(); ) {
				HostVersion hostVersion = host_list.get(i);
				if (hostVersion.version != version) {
					host_list.remove(hostVersion);
					--count;
				} else {
					++i;
				}
			}
			lock.unlock();
			return this;
		}
	}
	
	public enum TProtocolType {
		PERSISTENT,
		TEMPORARY
	}
	
	public class TProtocolWithType {
		TProtocol protocol;
		InetSocketAddress address;
		TProtocolType type;
		
		public TProtocol getProtocol() {
			return protocol;
		}
	}
	
	// 该类是一个单例类
	private static ZkNiftyTransportManager _instance = null;
	
	// 存放Service到可用transport的映射
	private Map<String, ZkNiftyList<TProtocolWithType>> _client_map = null;
	// 存放service到address的映射，包含了更新到的版本，用于判断哪些address已经失效
	private Map<String, ServiceVersion> _service_address_map = null;
	// 用于存放service名称到thrift对应client类的映射
	private Map<String, Class<? extends TServiceClient>> _service_class_map = null;

	// 客户端配置类
	private ZkNiftyClientConfig _config;
	
	// 使用nifty的client类进行网络通信和事件管理
	private NiftyClient _niftyClient = null;
	
	// 用于client端查询zookeeper以获取service信息的对象，会单独启动一个线程
	private ZookeeperRPCMutilServerAddressProvider _provider = null;
	
	// 用于运行provider的线程
	private Thread _provider_thread = null;
	
	// 用于初始化NettyClient配置的对象
	private NettyClientConfigBuilder _nettyClientConfigBuilder = null;
	
	private DefaultZkNiftyClient _zkNiftyClient = new DefaultZkNiftyClient();
	
	public static ZkNiftyTransportManager getInstance(ZkNiftyClientConfig config) {
		if (_instance == null) {
			_instance = new ZkNiftyTransportManager(config);
		}
		
		return _instance;
	}
	
	private ZkNiftyTransportManager(ZkNiftyClientConfig config) {
		this._config = config;
		
		if (_client_map == null) {
			this._client_map = new Hashtable<String, ZkNiftyList<TProtocolWithType>>();
		}
		
		if (_service_address_map == null) {
			this._service_address_map = new Hashtable<String, ServiceVersion>();
		}
		
		if (_service_class_map == null) {
			_service_class_map = new Hashtable<String, Class<? extends TServiceClient>>();
		}
		
		if (_provider == null) {
			_provider = new ZookeeperRPCMutilServerAddressProvider(_config);
		}
		
		if (_nettyClientConfigBuilder == null) {
			_nettyClientConfigBuilder = new NettyClientConfigBuilder();
		}
		
		_nettyClientConfigBuilder.setBossThreadCount(config.getBossThreadCount());
		_nettyClientConfigBuilder.setWorkerThreadCount(config.getWorkerThreadCount());
		_nettyClientConfigBuilder.setBossThreadExecutor(Executors.newFixedThreadPool(config.getBossThreadCount()));
		_nettyClientConfigBuilder.setWorkerThreadExecutor(Executors.newFixedThreadPool(config.getWorkerThreadCount()));
		
		if (_niftyClient == null) {
			_niftyClient = new NiftyClient(_nettyClientConfigBuilder.build());
		}
		
		this._client_map.clear();
		this._service_address_map.clear();
		this._service_class_map.clear();
		
		// 注册回调类，用于处理zookeeper监控路径的变化
		_provider.registerCallback(new ZkNiftyDefaultCallback(_config));
	}
	
	public void run() {
		// 启动线程用于zookeeper的通信
		try {
			_provider.init();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("Provider initialize failed. Exception Message : [{}].", e.getMessage());
		}
	}
	
	public void stop() {
		try {
			if (_provider != null) {
				_provider.close();
			}
			
			if (_niftyClient != null) {
				_niftyClient.close();
			}
			
			if (_provider_thread != null) {
				_provider_thread.interrupt();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.warn("Stop provider thread failed.");
		}
	}
	
	public void removeList(String service_path) {
		ZkNiftyList<TProtocolWithType> list = _client_map.get(service_path);
		if (null == list) {
			return;
		}
		_client_map.remove(service_path);
	}
	
	public int registerClientClass(String service_path, Class<? extends TServiceClient> client_class) {
		try {
			_service_class_map.put(service_path, client_class);
		} catch (ClassCastException e) {
			log.error("There is client_class for service_path[{}] already.", service_path);
			return -1;
		} catch (Exception e) {
			log.error("There is some exception[{}].", e.getMessage());
			return -1;
		}
		
		return 0;
	}
	
	public TProtocolWithType getTransport(String service_path) {
		ZkNiftyList<TProtocolWithType> list = _client_map.get(service_path);
		if (null == list) {
			// 没有相应的transport可用，则建立一个临时transport
			log.debug("create a template transport for service{}.", service_path);
			return createTemporaryTransport(service_path);
		} 
		return list.getClient();
	}

	public void putTransport(String service_path, TProtocolWithType client) {
		if (client == null) {
			System.out.println("client is null.");
		}
		if (client.type == TProtocolType.TEMPORARY) {
			log.debug("free the temporary transport.");
			client.protocol.getTransport().close();
			return;
		}
		
		// 在放回链接的时候判断下这个链接对应的service是否可用，如果可用则放回；否则关闭
		ServiceVersion serviceVersion = _service_address_map.get(service_path);
		if (serviceVersion == null) {
			client.protocol.getTransport().close();
			log.debug("The service[{}] is unavailable.", service_path);
			return;
		}
		
		Boolean bPutBack = false;
		for (HostVersion hostVersion : serviceVersion.host_list) {
			if (hostVersion.getAddress().equals(client.address) && hostVersion.getVersion() == serviceVersion.getServiceVersion()) {
				bPutBack = true;
				break;
			}
		}
		
		if (bPutBack) {
			ZkNiftyList<TProtocolWithType> list = _client_map.get(service_path);
			if (null != list) {
				list.addClient(client);
			}
		} else {
			client.protocol.getTransport().close();
			log.debug("Close the persistent transport for service[{}] address[{}].", service_path, client.address);
		}
	}
	
	// 监控的路径发生变化时需要进行各个map的更新
	public void updateServiceAddressMap(String service_path, List<InetSocketAddress> list) {
		ServiceVersion serviceVersion = _service_address_map.get(service_path);
		if (serviceVersion == null) {		// 第一次获取到该path的service信息
			serviceVersion = createNewServiceVersion(service_path, list);
		} else {  // 更新当前SerivceVersion
			serviceVersion = updateServiceVersion(serviceVersion, list);
		}
		
		// 每次update后都进行清理
		serviceVersion = serviceVersion.cleanServiceVersion();
		System.out.println("updateServiceVersion list size is " + serviceVersion.host_list.size());
	}
	
	private ServiceVersion createNewServiceVersion(String path, List<InetSocketAddress> list) {
		ServiceVersion serviceVersion = new ServiceVersion(path);
		
		for (InetSocketAddress address : list) {
			// 创建新HostVersion节点
			HostVersion hostVersion = new HostVersion(address, serviceVersion.version);
			
			// 追加到对应的ServiceVersion内的链表内
			serviceVersion.addHostVersion(hostVersion);
			_service_address_map.put(path, serviceVersion);
			
			// create transport for the host, and the count of transports is defined in config
			if (createPersistentTransport(path, address) < 0) {
				log.warn("Create persistent transport for host[{}] Failed.", address);
			}
		}
		
		return serviceVersion;
	}
	
	private TProtocolWithType createTransport(String path, InetSocketAddress address, TProtocolType type) {
		log.debug("Create some new transports for address[{}].", address);
		Class<? extends TServiceClient> clientClass = _service_class_map.get(path);
		try {
			TProtocolWithType protocol = new TProtocolWithType();
			protocol.type = type;
			protocol.protocol = _zkNiftyClient.createProtocol(clientClass, _niftyClient, address);
			protocol.address = address;
			return protocol;
		} catch (Exception e) {
			log.warn("create transport failed. error message[{}].", e.getMessage());
			return null;
		}
	}
	
	private int createPersistentTransport(String path, InetSocketAddress address) {
		ZkNiftyList<TProtocolWithType> list = _client_map.get(path);
		
		int transport_count = _config.getTransportCount(path);
		for (int i = 0; i < transport_count; ++i) {
			TProtocolWithType client = createTransport(path, address, TProtocolType.PERSISTENT);
			if (client == null) {
				log.warn("Cannt create new TProtocol for service[{}] which address is [{}].", path, address);
				return -1;
			}
			
			if (list == null) {
				list = new ZkNiftyList<TProtocolWithType>();
				_client_map.put(path, list);
			}
			list.addClient(client);
		}
		
		return 0;
	}
	
	private ServiceVersion updateServiceVersion(ServiceVersion serviceVersion, List<InetSocketAddress> list) {
		serviceVersion.incServiceVersion();
		System.out.println("updateServiceVersion list size is " + serviceVersion.host_list.size());
		for (InetSocketAddress address : list) {
			if (serviceVersion.updateHostVersion(address) == CREATE_NEW_HOSTVERSION) {
				HostVersion hostVersion = new HostVersion(address, serviceVersion.getServiceVersion());
				serviceVersion.addHostVersion(hostVersion);
				createPersistentTransport(serviceVersion.service_path, address);
			}
		}
		
		return serviceVersion;
	}
	
	/**
	 * 当链接不够用时，会建立临时链接，使用完会自动释放
	 */
	private TProtocolWithType createTemporaryTransport(String path) {
		ServiceVersion serviceVersion = _service_address_map.get(path);
		if (serviceVersion == null) {
			log.warn("There is no service for {}.", path);
			return null;
		}
		
		HostVersion hostVersion = serviceVersion.getOneHostVersion();
		if (hostVersion == null) {
			log.warn("Create temporary transport failed.");
			return null;
		}
		
		return createTransport(path, hostVersion.getAddress(), TProtocolType.TEMPORARY);
		
	}
}
