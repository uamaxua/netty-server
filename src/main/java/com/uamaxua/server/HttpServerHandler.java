package com.uamaxua.server;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.uamaxua.server.info.ConnectionInfo;
import com.uamaxua.server.info.RequestInfo;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class HttpServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

	private static final AtomicInteger REQ_COUNTS = new AtomicInteger();
	private static final AtomicInteger ACTIVE_COUNTS = new AtomicInteger();
	private static final ConcurrentHashMap<String, RequestInfo> UNIQUE_IP_MAP = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, AtomicInteger> REDIRECT_MAP = new ConcurrentHashMap<>();
	private static final ConcurrentLinkedQueue<ConnectionInfo> CONNECTION_QUEUE = new ConcurrentLinkedQueue<>();

	private String ip;
	private Timestamp timestamp;
	private String uri;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ACTIVE_COUNTS.incrementAndGet();
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ACTIVE_COUNTS.decrementAndGet();
		ConnectionInfo connectionInfo = ctx.channel()
				.attr(TrafficHandler.CONNECTION_INFO).get();
		synchronized (CONNECTION_QUEUE) {
			if (connectionInfo != null && uri != null) {
				if (CONNECTION_QUEUE.size() > 15) {
					CONNECTION_QUEUE.remove();
				}
				connectionInfo.setIp(ip);
				connectionInfo.setTimestamp(timestamp);
				connectionInfo.setUri(uri);
				CONNECTION_QUEUE.add(connectionInfo);
			}
		}
		super.channelInactive(ctx);
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, HttpRequest req)
			throws Exception {
		REQ_COUNTS.incrementAndGet();
		// HTTP 1.1 100 continue
		if (HttpHeaders.is100ContinueExpected(req)) {
			ctx.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
		}

		timestamp = new Timestamp(new Date().getTime());
		ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress()
				.getHostAddress();
		uri = req.getUri();

		synchronized (UNIQUE_IP_MAP) {
			if (UNIQUE_IP_MAP.containsKey(ip)) {
				RequestInfo requestInfo = UNIQUE_IP_MAP.get(ip);
				requestInfo.setTimestamp(timestamp);
				requestInfo.incrementCounts();
			} else {
				UNIQUE_IP_MAP.put(ip, new RequestInfo(timestamp));
			}
		}

		String path = new QueryStringDecoder(uri).path().toLowerCase();

		switch (path) {
		case "/hello":
			helloRequest(ctx, req);
			break;
		case "/redirect":
			redirectRequest(ctx, req);
			break;

		case "/status":
			statusRequest(ctx, req);
			break;

		default:
			responseHttp404(ctx, req);
			break;
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
		super.exceptionCaught(ctx, cause);
	}

	private void sendResponse(ChannelHandlerContext ctx, HttpResponse res) {
		ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
	}

	/**
	 * Responses "Hello world" after delay
	 * 
	 * @param ctx
	 * @param req
	 */
	private void helloRequest(final ChannelHandlerContext ctx,
			final HttpRequest req) {
		int delay = 10;
		String content = "Hello world";
		final FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK,
				Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		res.headers().set(CONTENT_TYPE, "text/plain");
		res.headers().set(CONTENT_LENGTH, res.content().readableBytes());
		ctx.executor().schedule(() -> {
			sendResponse(ctx, res);
		}, delay, TimeUnit.SECONDS);
	}

	/**
	 * Redirects request to another URL
	 * 
	 * @param ctx
	 * @param req
	 */
	private void redirectRequest(final ChannelHandlerContext ctx,
			final HttpRequest req) {
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1,
				MOVED_PERMANENTLY);
		QueryStringDecoder decoder = new QueryStringDecoder(req.getUri());
		try {
			List<String> paramList = decoder.parameters().get("url");
			// If no URL, send HTTP_404
			if (paramList == null) {
				responseHttp404(ctx, req);
				return;
			}
			String urlStr = paramList.get(0);
			URL url = new URL(urlStr);
			res.headers().set(LOCATION, url);
			res.headers().set(CONTENT_LENGTH, res.content().readableBytes());
			sendResponse(ctx, res);

			synchronized (REDIRECT_MAP) {
				if (REDIRECT_MAP.containsKey(urlStr)) {
					REDIRECT_MAP.get(urlStr).incrementAndGet();
				} else {
					REDIRECT_MAP.put(urlStr, new AtomicInteger(1));
				}
			}
		} catch (MalformedURLException e) {
			//If URL is invalid
			String content = "Bad request: invalid URL";
			FullHttpResponse badRes = new DefaultFullHttpResponse(HTTP_1_1,
					HttpResponseStatus.BAD_REQUEST, Unpooled.copiedBuffer(
							content, CharsetUtil.UTF_8));
			badRes.headers().set(CONTENT_TYPE, "text/plain");
			badRes.headers().set(CONTENT_LENGTH,
					badRes.content().readableBytes());
			sendResponse(ctx, badRes);
		}
	}

	/**
	 * Responses HTTP_404: Page not found
	 * @param ctx
	 * @param req
	 */
	private void responseHttp404(final ChannelHandlerContext ctx,
			final HttpRequest req) {
		String content = "404: Page Not Found";
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND,
				Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		res.headers().set(CONTENT_TYPE, "text/plain");
		res.headers().set(CONTENT_LENGTH, res.content().readableBytes());
		sendResponse(ctx, res);
	}

	/**
	 * Responses status
	 * @param ctx
	 * @param req
	 */
	private void statusRequest(final ChannelHandlerContext ctx,
			final HttpRequest req) {
		StringBuilder content = new StringBuilder();
		content.append("<p>Общее количество запросов: </b>");
		content.append(REQ_COUNTS.get());
		content.append("</b></p>");
		content.append("<p>Количество уникальных запросов (по одному на IP): </b>");
		content.append(UNIQUE_IP_MAP.size());
		content.append("</b></p>");

		content.append("<table border=\"1\">");
		content.append("<caption>Таблица уникальных запросов</caption>");
		content.append("<tr> <th>Ip</th> <th>Кол-во запросов</th> <th>Время последнего запроса</th> </tr>");
		for (Entry<String, RequestInfo> entry : UNIQUE_IP_MAP.entrySet()) {
			content.append("<tr> <td>");
			content.append(entry.getKey());
			content.append("</td> <td>");
			content.append(entry.getValue().getReqCounts());
			content.append("</td> <td>");
			content.append(entry.getValue().getTimestamp());
		}
		content.append("</td> </tr> </table>  <br>");

		content.append("<table border=\"1\">");
		content.append("<caption>Таблица переадресаций</caption>");
		content.append("<tr> <th>URL</th> <th>Количество переадресаций</th> </tr>");
		for (Entry<String, AtomicInteger> entry : REDIRECT_MAP.entrySet()) {
			content.append("<tr><td>");
			content.append(entry.getKey());
			content.append("</td><td>");
			content.append(entry.getValue().get());
		}
		content.append("</td></tr></table><br>");

		content.append("<p>Количество соединений, открытых в данный момент: <b>");
		content.append(ACTIVE_COUNTS.get());
		content.append("</b></p><br>");

		content.append("<table border=\"1\">");
		content.append("<caption>Taблица 16 последних обработанных соединений</caption>");
		content.append("<tr> <th>src_ip</th> <th>URI</th> <th>timestamp</th> <th>sent_bytes</th> <th>received_bytes</th> <th>speed (bytes/sec)</th> </tr>");
		for (ConnectionInfo connectionInfo : CONNECTION_QUEUE) {
			content.append("<tr><td>");
			content.append(connectionInfo.getIp());
			content.append("</td><td>");
			content.append(connectionInfo.getUri());
			content.append("</td><td>");
			content.append(connectionInfo.getTimestamp());
			content.append("</td><td>");
			content.append(connectionInfo.getSentBytes());
			content.append("</td><td>");
			content.append(connectionInfo.getReceivedBytes());
			content.append("</td><td>");
			content.append(new DecimalFormat("#").format(connectionInfo
					.getSpeed()));
		}
		content.append("</td></tr></table><br>");

		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK,
				Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
		res.headers().set(CONTENT_LENGTH, res.content().readableBytes());
		sendResponse(ctx, res);
	}

}
