package com.moji.zookeepernifty;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;  
import org.w3c.dom.Element;  
import org.w3c.dom.Node;  
import org.w3c.dom.NodeList; 
import org.xml.sax.SAXException;

public class ZkNiftyConfigXml implements ZkNiftyConfig {
	private static final Logger log = LoggerFactory.getLogger(ZkNiftyConfigXml.class);
	
	protected String _config_path;
	
	public ZkNiftyConfigXml(String config_path){
		_config_path = config_path;
	}
	
	@Override
	public int load() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = dbf.newDocumentBuilder();
			InputStream in = getClass().getResourceAsStream(this._config_path);
			Document document = builder.parse(in);
			// root
			Element root = document.getDocumentElement();
			if (root == null) {
				log.error("The configure file[{}] parse failed, there is not root.", _config_path);
				return -1;
			}
			
			// all node parse
			NodeList node_list = root.getChildNodes();
			if (node_list == null) {
				log.error("The configure file[{}] has only root node.", _config_path);
				return -1;
			}
			
			for (int i = 0; i < node_list.getLength(); ++i) {
				Node node = node_list.item(i);
				if (parseNode(node) < 0) {
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
	
	public int parseNode(Node node) {
		return 0;
	}

}
