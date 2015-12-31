package com.uamaxua.server.info;

import java.sql.Timestamp;

public class ConnectionInfo {
	private String ip;
	private String uri;
	private Timestamp timestamp;
	private long sentBytes;
	private long receivedBytes;
	private double speed;
			
	public ConnectionInfo() {
	}

	public ConnectionInfo(String ip, String uri, Timestamp timestamp,
			long sentBytes, long receivedBytes, double speed) {
		super();
		this.ip = ip;
		this.uri = uri;
		this.timestamp = timestamp;
		this.sentBytes = sentBytes;
		this.receivedBytes = receivedBytes;
		this.speed = speed;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	public long getSentBytes() {
		return sentBytes;
	}

	public void setSentBytes(long sentBytes) {
		this.sentBytes = sentBytes;
	}

	public long getReceivedBytes() {
		return receivedBytes;
	}

	public void setReceivedBytes(long receivedBytes) {
		this.receivedBytes = receivedBytes;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result
				+ (int) (receivedBytes ^ (receivedBytes >>> 32));
		result = prime * result + (int) (sentBytes ^ (sentBytes >>> 32));
		long temp;
		temp = Double.doubleToLongBits(speed);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConnectionInfo other = (ConnectionInfo) obj;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		if (receivedBytes != other.receivedBytes)
			return false;
		if (sentBytes != other.sentBytes)
			return false;
		if (Double.doubleToLongBits(speed) != Double
				.doubleToLongBits(other.speed))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ConnectionInfo [ip=" + ip + ", uri=" + uri + ", timestamp="
				+ timestamp + ", sentBytes=" + sentBytes + ", receivedBytes="
				+ receivedBytes + ", speed=" + speed + "]";
	}
}
