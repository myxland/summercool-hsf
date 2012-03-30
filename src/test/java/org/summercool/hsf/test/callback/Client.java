package org.summercool.hsf.test.callback;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.summercool.hsf.netty.channel.HsfChannel;
import org.summercool.hsf.netty.service.HsfConnector;
import org.summercool.hsf.netty.service.HsfConnectorImpl;
import org.summercool.hsf.proxy.ServiceProxyFactory;
import org.summercool.hsf.test.listener.TestChannelListener;
import org.summercool.hsf.test.service.TestService;
import org.summercool.hsf.util.AsyncCallback;
import org.summercool.hsf.util.HsfOptions;

public class Client {
	private static TestAsyncCallback testAsyncCallback = new TestAsyncCallback();

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		HsfConnector connector = new HsfConnectorImpl();
		connector.setOption(HsfOptions.HOLD_CALLBACK_MESSAGE, false);
		connector.connect(new InetSocketAddress("192.168.1.40", 8082));

		final TestService testService = ServiceProxyFactory.getRoundFactoryInstance(connector).wrapAsyncCallbackProxy(
				TestService.class, testAsyncCallback);

		ExecutorService executorService = Executors.newFixedThreadPool(150);
		long begin = System.currentTimeMillis();

		for (int i = 0; i < 1000000; i++) {
			final int j = i;
			executorService.execute(new Runnable() {

				@Override
				public void run() {
					try {
						testService.test("大家都有过复制一个大文件时，久久等待却不见结束，明明很着急却不能取消的情况吧——一旦取消，一切都要从头开始！在Windows 8");
					} catch (Exception e) {
//						System.err.println("error while i = " + j + " " + e.getMessage());
					}
				}
			});
		}
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.DAYS);

		long end = System.currentTimeMillis();
		System.out.println("cost:" + (end - begin) + "ms");
	}

	public static class TestAsyncCallback extends AsyncCallback<Object> {
		public void doCallback(Object data) {
			//System.out.println(data);
		}
		
		@Override
		public void doExceptionCaught(Throwable ex, HsfChannel channel, Object param) {
			System.out.println(param);
//			super.doExceptionCaught(ex, channel, param);
		}
	}
}
