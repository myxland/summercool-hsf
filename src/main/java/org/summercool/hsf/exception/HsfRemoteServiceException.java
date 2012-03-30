package org.summercool.hsf.exception;

/**
 * @Title: HsfRemoteServiceException.java
 * @Package org.summercool.hsf.exception
 * @Description: Hsf远程调用异常
 * @author 简道
 * @date 2011-9-16 下午12:12:47
 * @version V1.0
 */
public class HsfRemoteServiceException extends RuntimeException {
	private static final long serialVersionUID = 5822623760553747361L;

	public HsfRemoteServiceException() {
		super();
	}

	public HsfRemoteServiceException(String message) {
		super(message.length() > 4096 ? message.substring(0, 4093) + "..." : message);
	}

	public HsfRemoteServiceException(String message, Throwable cause) {
		super(message.length() > 4096 ? message.substring(0, 4093) + "..." : message, cause);
	}

	public HsfRemoteServiceException(Throwable cause) {
		super(cause);
	}
}
