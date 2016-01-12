package com.moji.zookeepernifty;

import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;  
import org.w3c.dom.Node;  
import org.w3c.dom.NodeList; 
import org.xml.sax.SAXException;

public class ZkNiftyClientConfig extends Object {

	private static final Logger log = LoggerFactory.getLogger(ZkNiftyClientConfig.class);
	
	private static final int DEFAULT_TRANSPORT_COUNT = 10;
	private static final int DEFAULT_TRANSPORT_MAX_IDLE = 5;
	private static final int DEFAULT_TRANSPORT_MIN_IDLE = 5;
	
	private static final String ZKNIFTYCLIENT = "ZkNiftyClient";
	private static final String ZOOKEEPERADDRESS = "ZookeeperAddress";
	private static final String BOSSTHREADCOUNT = "BossThreadCount";
	private static final String WORKERTHREADCOUNT = "WorkerThreadCount";
	private static final String SERVICEINFORMATIONS = "ServiceInformations";
	
	private static final String SERVICENAME = "service_name";
	private static final String SERVICEVERSION = "service_version";
	
	private static final String	CONNECTION_MAX_TOTAL = "maxTotal";
	private static final String	CONNECTION_MAX_IDLE = "maxIdle";
	private static final String	CONNECTIOn_MIN_IDLE = "minIdle";
	
	private static final int BOSS_THREAD_DEFAULT_COUNT = 1;
	private static final int WORKER_THREAD_DEFAULT_COUNT = 4;
	
	private String _config_path;
	private String _zookeeper_address;
	private int _boss_thread_count = BOSS_THREAD_DEFAULT_COUNT;
	private int _worker_thread_count = WORKER_THREAD_DEFAULT_COUNT;
	private int _service_count = 0;
	private List<String> _service_path_list = new ArrayList<String>();
	private Map<String, ZkNiftyClientPoolCount> _service_transport_map = new Hashtable<String, ZkNiftyClientPoolCount>();
	public ZkNiftyClientConfig(String config_path) {
		_config_path = config_path;
		_service_count = 0;
	}
	
	public ZkNiftyClientConfig() {
		
	}

	public int load() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document document = builder.parse(this._config_path);
			// root
			document.getDocumentElement().normalize();
			
			Node root = document.getFirstChild();
			while (true) {
				if (root == null) {
					log.error("The configure file[{}] parse failed, there is not root.", _config_path);
					return -1;
				}
 
				if (root.getNodeType() == Node.ELEMENT_NODE) { 
					if (root.getAttributes().getNamedItem("name").getNodeValue().equals(ZKNIFTYCLIENT)) {
						break;
					}
				}
				root = root.getNextSibling();
			}
			
			// all node parse
			NodeList node_list = root.getChildNodes();
			if (node_list == null) {
				log.error("The configure file[{}] has only root node.", _config_path);
				return -1;
			}
			
			for (int i = 0; i < node_list.getLength(); ++i) {
				Node node = node_list.item(i);
				if (node != null && node.getNodeType() == Node.ELEMENT_NODE && parseNode(node) < 0) {
					return -1;
				}
			}
			
		} catch (ParserConfigurationException e) {  
            e.printStackTrace();  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        } catch (SAXException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } 
		return 0;
	}
	
	private int parseNode(Node node) {
		try {
			String key = node.getAttributes().getNamedItem("name").getNodeValue();
			if (key.equals(ZOOKEEPERADDRESS)) {
				_zookeeper_address = node.getAttributes().getNamedItem("address").getNodeValue();
			} else if (key.equals(BOSSTHREADCOUNT)) {
				_boss_thread_count = Integer.parseInt(node.getAttributes().getNamedItem("count").getNodeValue());
			} else if (key.equals(WORKERTHREADCOUNT)) {
				_worker_thread_count = Integer.parseInt(node.getAttributes().getNamedItem("count").getNodeValue());
			} else if (key.equals(SERVICEINFORMATIONS)) {
				NodeList node_list = node.getChildNodes();
				if (node_list == null) {
					log.error("There is no available service defined in configure file.");
					return -1;
				}
				
				for (int i = 0; i < node_list.getLength(); ++i) {
					Node child_node = node_list.item(i);
					if (child_node != null && child_node.getNodeType() == Node.ELEMENT_NODE) {
						String service_name = child_node.getAttributes().getNamedItem(SERVICENAME).getNodeValue();
						String service_version = child_node.getAttributes().getNamedItem(SERVICEVERSION).getNodeValue();
						Integer maxTotal = Integer.parseInt(child_node.getAttributes().getNamedItem(CONNECTION_MAX_TOTAL).getNodeValue());
						Integer maxIdle = Integer.parseInt(child_node.getAttributes().getNamedItem(CONNECTION_MAX_IDLE).getNodeValue());
						Integer minIdle = Integer.parseInt(child_node.getAttributes().getNamedItem(CONNECTIOn_MIN_IDLE).getNodeValue());
						ZkNiftyClientPoolCount poolCount = new ZkNiftyClientPoolCount(maxTotal, maxIdle, minIdle);
						_service_transport_map.put("/" + service_name + "/" + service_version, poolCount);
						
						_service_path_list.add("/" + service_name + "/" + service_version);
						
						++_service_count;
					}
				}
			}
			
			return 0;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			log.error("Translate to int Failed while parse configure. Error message[{}].", e.getMessage());
			return -1;
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Parse configure file failed. Error message[{}].", e.getMessage());
			return -1;
		}
		
	}
	
	public String getZookeeper() {
		return _zookeeper_address;
	}
	
	public int getBossThreadCount() {
		return _boss_thread_count;
	}
	
	public int getWorkerThreadCount() {
		return _worker_thread_count;
	}
	
	public int getServiceCount() {
		return _service_count;
	}
	
	public List<String> getServicePathList() {
		return _service_path_list;
	}
	
	public ZkNiftyClientPoolCount getTransportPoolCount(String path) {
		if (_service_transport_map.containsKey(path)) {
			return _service_transport_map.get(path);
		}
		return new ZkNiftyClientPoolCount(DEFAULT_TRANSPORT_COUNT, DEFAULT_TRANSPORT_MAX_IDLE, DEFAULT_TRANSPORT_MIN_IDLE);
	}

	public String get_zookeeper_address() {
		return _zookeeper_address;
	}

	public void set_zookeeper_address(String _zookeeper_address) {
		this._zookeeper_address = _zookeeper_address;
	}

	public int get_boss_thread_count() {
		return _boss_thread_count;
	}

	public void set_boss_thread_count(int _boss_thread_count) {
		this._boss_thread_count = _boss_thread_count;
	}

	public int get_worker_thread_count() {
		return _worker_thread_count;
	}

	public void set_worker_thread_count(int _worker_thread_count) {
		this._worker_thread_count = _worker_thread_count;
	}
	
	public Map<String, ZkNiftyClientPoolCount> get_service_transport_map() {
		return _service_transport_map;
	}

	public void set_service_transport_map(
			Map<String, ZkNiftyClientPoolCount> _service_transport_map) {
		this._service_transport_map = _service_transport_map;
	}

	public List<String> get_service_path_list() {
		return _service_path_list;
	}

	public void set_service_path_list(List<String> _service_path_list) {
		this._service_path_list = _service_path_list;
	}

}
