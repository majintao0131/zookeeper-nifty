package com.moji.zookeepernifty;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CommonXmlParser {
	private static final Logger log = LoggerFactory.getLogger(CommonXmlParser.class);
	
	private String _config_path;
	private String _auth_string;
	private Map<String, String> _config_map = new Hashtable<String, String>();
	
	public int load(String config_path, String auth_string) {
		_config_path = config_path;
		_auth_string = auth_string;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {	
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document document = builder.parse(this._config_path);
			document.getDocumentElement().normalize();
			
			// root
			Node root = getRoot(document);
			
			// all node parse
			if (parseDocument(root) < 0) {
				log.error("Load configure file[{}] failed.", _config_path);
				return -1;
			}
		} catch (ParserConfigurationException e) {  
            e.printStackTrace();  
            return -1;
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
            return -1;
        } catch (SAXException e) {  
            e.printStackTrace();  
            return -1;
        } catch (IOException e) {  
            e.printStackTrace();  
            return -1;
        } 

		return 0;
	}
	
	private Node getRoot(Document document) {
		Node root = document.getFirstChild();
		while (true) {
			if (root == null) {
				log.error("The configure file[{}] parse failed, there is not root.", this._config_path);
				return null;
			}
			System.out.println(root.getAttributes().getNamedItem("name").getNodeValue() + "\t" + root.getNodeType());
			if (root.getNodeType() == Node.ELEMENT_NODE) { 
				if (root.getAttributes().getNamedItem("name").getNodeValue().equals(_auth_string)) {
					break;
				}
			}
			root = root.getNextSibling();
		}
		
		return root;
	}
	
	private int parseDocument(Node root) {
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
		
		return 0;
	}
	
	private int parseNode(Node node) {
		try {
			String key = node.getAttributes().getNamedItem("name").getNodeValue();
			String value = node.getAttributes().getNamedItem("address").getNodeValue();
			_config_map.put(key, value);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			log.error("Translate to int Failed while parse configure. Error message[{}].", e.getMessage());
			return -1;
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Parse configure file failed. Error message[{}].", e.getMessage());
			return -1;
		}
		
		return 0;
	}
	
	public String getConfigure(String key) {
		return _config_map.get(key);
	}
}

