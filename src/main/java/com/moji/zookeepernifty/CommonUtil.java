package com.moji.zookeepernifty;

import java.net.InetAddress;

public class CommonUtil {
	/** 
     * 或者主机名： 
     * @return 
     */ 
    public static String getLocalHostName() { 
         String hostName; 
         try { 
              /**返回本地主机。*/ 
              InetAddress addr = InetAddress.getLocalHost(); 
              /**获取此 IP 地址的主机名。*/ 
              hostName = addr.getHostName(); 
         }catch(Exception ex){ 
             hostName = ""; 
         } 
           
         return hostName; 
    } 
    /** 
     * 获得本地所有的IP地址 
     * @return 
     */ 
	public static String getLocalHostIP() {
		String address = null;
		String host_name = getLocalHostName();
		try {
			for (InetAddress a : InetAddress.getAllByName(host_name)) {
				if (!a.isLinkLocalAddress() && !a.isLoopbackAddress()) {
					address = a.getHostAddress();
					break;
				}
			}
		} catch (Exception ex) {
			return null;
		}
		return address;
	}
}
