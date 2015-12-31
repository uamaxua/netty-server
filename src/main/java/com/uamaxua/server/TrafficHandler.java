package com.uamaxua.server;

import com.uamaxua.server.info.ConnectionInfo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.AttributeKey;

public class TrafficHandler extends ChannelTrafficShapingHandler {

	public static final AttributeKey<ConnectionInfo> CONNECTION_INFO = AttributeKey
			.valueOf("TrafficHandler.attr");

	public TrafficHandler(long checkInterval) {
		super(checkInterval);
	}
		
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.setReceivedBytes(trafficCounter().cumulativeReadBytes());
		connectionInfo.setSentBytes(trafficCounter().cumulativeWrittenBytes());
		connectionInfo.setSpeed(calculateSpeed());
		ctx.channel().attr(CONNECTION_INFO).set(connectionInfo);
		super.channelInactive(ctx);
	}
	
	/**
	 * Calculates speed of processing data for one request. If time of the
	 * processing data < 1 ms returns Infinity
	 * 
	 * @return - speed (byte / sec)
	 **/
	private double calculateSpeed() {
		long time = System.currentTimeMillis()
				- trafficCounter().lastCumulativeTime();
		return (double) (trafficCounter().cumulativeReadBytes() + trafficCounter()
				.cumulativeWrittenBytes() * 1000) / time;
	}

}
