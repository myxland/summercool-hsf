package org.summercool.hsf.util;

/**
 * 
 * @author 简道
 */
public class CallbackRegister {
	private static final String CALLBACK = "CALLBACK_";
	private static final String CALLBACK_PARAM = "CALLBACK_PARAM_";
	private static final String CALLBACK_DATA = "CALLBACK_DATA_";

	public static void setCallbackData(Object data) {
		TLSUtil.setData(CALLBACK_DATA, data);
	}

	public static Object getCallbackData() {
		return TLSUtil.getData(CALLBACK_DATA);
	}

	public static void clearCallbackData() {
		TLSUtil.remove(CALLBACK_DATA);
	}

	public static <T> void setCallback(AsyncCallback<T> callback) {
		TLSUtil.setData(CALLBACK, callback);
	}

	@SuppressWarnings("rawtypes")
	public static AsyncCallback getCallback() {
		return (AsyncCallback) TLSUtil.getData(CALLBACK);
	}

	public static void clearCallback() {
		TLSUtil.remove(CALLBACK);
	}

	public static <T> void setCallbackParam(Object param) {
		TLSUtil.setData(CALLBACK_PARAM, param);
	}

	public static Object getCallbackParam() {
		return TLSUtil.getData(CALLBACK_PARAM);
	}

	public static void clearCallbackParam() {
		TLSUtil.remove(CALLBACK_PARAM);
	}
}