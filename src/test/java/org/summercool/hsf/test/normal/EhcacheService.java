package org.summercool.hsf.test.normal;

import org.summercool.hsf.annotation.RemoteServiceContract;

@RemoteServiceContract
public interface EhcacheService {

	public void setState(String key, State state);

	public State getState(String key);

}
