package com.uamaxua.server.info;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestInfo {
	private AtomicInteger reqCounts = new AtomicInteger();
	private Timestamp timestamp;
	
	public RequestInfo(Timestamp timestamp) {
		super();
		reqCounts.incrementAndGet();
		this.timestamp = timestamp;
	}

	public int getReqCounts() {
		return reqCounts.get();
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(Timestamp timestamp){
		this.timestamp = timestamp;
	}
	
	public void incrementCounts(){
		reqCounts.incrementAndGet();
	}
	
}
