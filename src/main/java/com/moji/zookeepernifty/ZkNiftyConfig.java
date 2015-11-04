package com.moji.zookeepernifty;

import org.w3c.dom.Node;  

public interface ZkNiftyConfig {
	public int load();
	public int parseNode(Node node);
}
