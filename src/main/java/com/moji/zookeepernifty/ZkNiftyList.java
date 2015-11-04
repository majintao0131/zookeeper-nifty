package com.moji.zookeepernifty;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ZkNiftyList<T> {
	private Queue<T> queue = new ConcurrentLinkedQueue<T>();
	
	public ZkNiftyList() {
		queue.clear();
	}
	
	public void addClient(T client) {
		queue.offer(client);
	}
	
	public void removeClient(T client) {
		queue.remove(client);
	}
	
	public void removeAll() {
		queue.clear();
	}
	
	public T getClient() {
		return queue.poll();
	}
}