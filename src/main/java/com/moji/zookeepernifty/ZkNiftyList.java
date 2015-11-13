package com.moji.zookeepernifty;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ZkNiftyList<T> {
	private Queue<T> queue = new ConcurrentLinkedQueue<T>();
	private Lock lock = null;
	
	public ZkNiftyList() {
		if (lock == null) {
			lock = new ReentrantLock();
		}
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
	
	public int getCount() {
		return queue.size();
	}
	
	public Queue<T> getQuene() {
		return queue;
	}
	
	public void Lock() {
		this.lock.lock();
	}
	
	public void UnLock() {
		this.lock.unlock();
	}
}