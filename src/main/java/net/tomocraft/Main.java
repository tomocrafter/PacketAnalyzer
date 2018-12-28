package net.tomocraft;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.internal.SocketUtils;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Main extends SimpleChannelInboundHandler<DatagramPacket> {

	private static final byte[] OFFLINE_MESSAGE_DATA_ID = new byte[]{(byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78};

	private Channel channel;

	private Main() {
		try {
			EventLoopGroup group = Epoll.isAvailable() ? new EpollEventLoopGroup()
					: KQueue.isAvailable() ? new KQueueEventLoopGroup()
					: new NioEventLoopGroup();

			Bootstrap bootstrap = new Bootstrap()
					.group(group)
					.channel(Epoll.isAvailable() ? EpollDatagramChannel.class
							: KQueue.isAvailable() ? KQueueDatagramChannel.class
							: NioDatagramChannel.class)
					.handler(this);

			channel = bootstrap.bind(0).sync().channel();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		ByteBuf buf = Unpooled.buffer();
		buf.writeByte((byte) 0x01);

		long pingId = new Random().nextLong();
		System.out.println("Sending PingID: " + pingId);
		buf.writeLong(pingId);

		buf.writeLong(System.currentTimeMillis());
		buf.writeBytes(OFFLINE_MESSAGE_DATA_ID);

		channel.writeAndFlush(new DatagramPacket(buf, SocketUtils.socketAddress("sg.lbsg.net", 19132)));
	}

	public static void main(String[] args) {
		new Main();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
		ByteBuf content = msg.content();
		System.out.println("PacketID: " + content.readByte());
		System.out.println("PingID: " + content.readLong());
		System.out.println("ServerID: " + content.readLong());
		content.readBytes(16);//Skip the OFFLINE_MESSAGE_DATA_ID
		short length = content.readShort();
		byte[] bytes = new byte[length];
		content.readBytes(bytes);
		System.out.println("ServerName: " + new String(bytes, StandardCharsets.UTF_8));

		System.out.println("Is still byte left?: " + (content.isReadable() ? "yes" : "no"));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
	}
}
