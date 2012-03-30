package org.summercool.hsf.serializer;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.RegisteredClass;
import com.esotericsoftware.kryo.ObjectBuffer;
import com.esotericsoftware.kryo.serialize.BigDecimalSerializer;

/**
 * @ClassName: KryoSerializer
 * @Description: Kryo的序列化和反序列化实现类
 * @author 简道
 * @date 2011-9-16 下午12:06:15
 * 
 */
public class KryoSerializer implements Serializer {

	private Kryo kryo;
	/**
	 * @Fields initialCapacity : 初始容量
	 */
	private int initialCapacity = 512;
	/**
	 * @Fields maxCapacity : 最大容量
	 */
	private int maxCapacity = 5 * 1024 * 1024;

	private List<Class<?>> registeredClass;

	public KryoSerializer() {
		this.kryo = new Kryo();
		kryo.register(BigDecimal.class, new BigDecimalSerializer());
		kryo.setRegistrationOptional(true);

	}

	public void init() throws ClassNotFoundException {
		if (registeredClass != null) {
			for (Class<?> clazz : registeredClass) {
				register(clazz);
			}
		}
	}

	public RegisteredClass register(Class<?> type, com.esotericsoftware.kryo.Serializer serializer) {
		return kryo.register(type, serializer);
	}

	public void register(Class<?> clazz) {
		kryo.register(clazz);
	}

	public byte[] serialize(Object object) {
		ObjectBuffer buffer = new ObjectBuffer(kryo, initialCapacity, maxCapacity);
		return buffer.writeClassAndObject(object);
	}

	public Object deserialize(byte[] bytes) throws Exception {
		ObjectBuffer buffer = new ObjectBuffer(kryo, initialCapacity, maxCapacity);
		return buffer.readClassAndObject(bytes);
	}

	public void setRegisteredClass(List<Class<?>> registeredClass) {
		this.registeredClass = registeredClass;
	}

	public void setRegisteredClass(Class<?>... registeredClass) {
		if (registeredClass != null) {
			this.registeredClass = Arrays.asList(registeredClass);
		}
	}

	public void setRegistrationOptional(boolean registrationOptional) {
		kryo.setRegistrationOptional(registrationOptional);
	}

	public void setInitialCapacity(int initialCapacity) {
		this.initialCapacity = initialCapacity;
	}

	public void setMaxCapacity(int maxCapacity) {
		this.maxCapacity = maxCapacity;
	}
}
