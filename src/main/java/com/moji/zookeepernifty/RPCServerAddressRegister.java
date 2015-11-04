package com.moji.zookeepernifty;

import java.io.Closeable;

public interface RPCServerAddressRegister extends Closeable {
	public int register(String service, String version, String address);
}
