package org.summercool.hsf.test.normal;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.summercool.hsf.netty.listener.EventBehavior;
import org.summercool.hsf.netty.listener.ExceptionEventListener;
import org.summercool.hsf.netty.service.HsfAcceptor;
import org.summercool.hsf.netty.service.HsfAcceptorImpl;
import org.summercool.hsf.util.HsfOptions;
import org.summercool.hsf.util.StackTraceUtil;

/**
 * @Title: Server.java
 * @Package com.gexin.hsf2.normal
 * @Description: TODO(添加描述)
 * @date 2012-2-23 上午12:58:53
 * @version V1.0
 */
public class Server {

	public static void main(String[] args) {
		
		List<EventListener> listeners = new ArrayList<EventListener>();
		//
		ExceptionEventListener exceptionListener = new ExceptionEventListener(){
			@Override
			public EventBehavior exceptionCaught(ChannelHandlerContext ctx, Channel channel, ExceptionEvent e) {
				System.out.println( " ABC : " +  StackTraceUtil.getStackTrace(e.getCause()));
				return EventBehavior.Continue;
			}
		};
		listeners.add(exceptionListener);
		
		HsfAcceptor acceptor = new HsfAcceptorImpl();
//		acceptor.setOption(HsfOptions.OPEN_SERVICE_INVOKE_STATISTIC, true);
		acceptor.setOption(HsfOptions.MAX_THREAD_NUM_OF_DISPATCHER, 150);
		acceptor.registerService(new EhcacheServiceImpl());
		acceptor.setListeners(listeners);
		
		acceptor.bind(8082);
	}
}
