package com.moji.zookeepernifty;

public class ZkNiftyException  extends RuntimeException{
	
	private static final long serialVersionUID = 1L;

	public ZkNiftyException(){
		super();
	}
	
	public ZkNiftyException(String msg){
		super(msg);
	}
	
	public ZkNiftyException(Throwable e){
		super(e);
	}
	
	public ZkNiftyException(String msg,Throwable e){
		super(msg,e);
	}
}

