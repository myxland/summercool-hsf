package org.summercool.hsf.netty.channel;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class FlowManagerImpl implements FlowManager {
	private int threshold = 2000000;
	private Semaphore available = new Semaphore(threshold, false);
	private ReentrantLock lock = new ReentrantLock();

	@Override
	public void acquire() throws InterruptedException {
		available.acquire();
	}

	@Override
	public void acquire(int permits) throws InterruptedException {
		available.acquire(permits);
	}

	@Override
	public boolean acquire(int permits, int timeout) {
		try {
			return available.tryAcquire(permits, timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}

	@Override
	public void release() {
		if (available.availablePermits() < threshold) {
			try {
				lock.lock();

				if (available.availablePermits() < threshold) {
					available.release();
				}
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	public int getAvailable() {
		return available.availablePermits();
	}

	@Override
	public void setThreshold(int newThreshold) {
		if (newThreshold <= 0) {
			throw new IllegalArgumentException("threshold must great than 0.");
		}

		try {
			lock.lock();

			if (newThreshold > threshold) {
				int offset = newThreshold - threshold;
				threshold = newThreshold;
				available.release(offset);
			} else {
				int offset = threshold - newThreshold;
				threshold = newThreshold;
				try {
					available.acquire(offset);
				} catch (InterruptedException e) {
				}
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int getThreshold() {
		return threshold;
	}
}
