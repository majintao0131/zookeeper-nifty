package com.moji.zookeepernifty;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document; 
import org.w3c.dom.Node;  
import org.w3c.dom.NodeList; 
import org.xml.sax.SAXException;

public class ZkNiftyServerConfig extends Object {

	private static final Logger log = LoggerFactory.getLogger(ZkNiftyServerConfig.class);
	
	private static final String ZKNIFTYSERVER = "ZkNiftyServer";
	private static final String ZOOKEEPERADDRESS = "ZookeeperAddress";
	private static final String BOSSTHREADCOUNT = "BossThreadCount";
	private static final String WORKERTHREADCOUNT = "WorkerThreadCount";
	
	private static final String SERVICENAME = "ServiceName";
	private static final String SERVICEVERSION = "ServiceVersion";
	
	private static final int BOSS_THREAD_DEFAULT_COUNT = 1;
	private static final int WORKER_THREAD_DEFAULT_COUNT = 4;
	
	private String _config_path;
	
	private String _service_name;
	private String _service_version;
	private String _zookeeper_address;
	private int _boss_thread_count = BOSS_THREAD_DEFAULT_COUNT;
	private int _worker_thread_count = WORKER_THREAD_DEFAULT_COUNT;
	
	public ZkNiftyServerConfig(String config_path) {
		_config_path = config_path;
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
					log.error("The configure file[{}] parse failed, there is not root.", this._config_path);
					return -1;
				}
				System.out.println(root.getAttributes().getNamedItem("name").getNodeValue() + "\t" + root.getNodeType());
				if (root.getNodeType() == Node.ELEMENT_NODE) { 
					if (root.getAttributes().getNamedItem("name").getNodeValue().equals(ZKNIFTYSERVER)) {
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
			} else if (key.equals(SERVICENAME)) {
				_service_name = node.getAttributes().getNamedItem("value").getNodeValue();
			} else if (key.equals(SERVICEVERSION)) {
				_service_version = node.getAttributes().getNamedItem("value").getNodeValue();
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
	
	public String getServiceName() {
		return _service_name;
	}
	
	public String getServiceVersion() {
		return _service_version;
	}

}
