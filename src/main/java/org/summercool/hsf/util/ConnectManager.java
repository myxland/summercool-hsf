package org.summercool.hsf.util;

import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Title: ReconnectManager.java
 * @Package org.summercool.hsf.util
 * @Description: 重连管理类
 * @author 简道
 * @date 2011-11-17 下午8:33:48
 * @version V1.0
 */
public class ConnectManager {
	private ConcurrentHashMap<SocketAddress, AtomicInteger> disconnAddressList = new ConcurrentHashMap<SocketAddress, AtomicInteger>();
	private ConcurrentHashMap<SocketAddress, String> connectedAddressList = new ConcurrentHashMap<SocketAddress, String>();

	public Set<SocketAddress> getConnectedAddress() {
		return connectedAddressList.keySet();
	}

	public String getConnectedGroupName(SocketAddress address) {
		return connectedAddressList.get(address);
	}

	public void addConnected(SocketAddress address, String group) {
		connectedAddressList.put(address, group);
	}

	public void removeConnected(SocketAddress address) {
		connectedAddressList.remove(address);
	}

	public Set<SocketAddress> getDisconnectAddress() {
		return disconnAddressList.keySet();
	}

	public void addDisconnectAddress(SocketAddress address) {
		AtomicInteger num = disconnAddressList.putIfAbsent(address, new AtomicInteger(1));

		if (num != null) {
			num.getAndIncrement();
		}
	}

	public void countDownDisconnect(SocketAddress address) {
		AtomicInteger num = disconnAddressList.get(address);
		if (num != null) {
			if (num.decrementAndGet() < 1) {
				removeDisconnect(address);
			}
		}
	}

	public void removeDisconnect(SocketAddress address) {
		disconnAddressList.remove(address);
	}

	public Integer getDisconnectNum(SocketAddress address) {
		AtomicInteger num = disconnAddressList.get(address);
		if (num == null) {
			return 0;
		}
		return num.get();
	}
}
