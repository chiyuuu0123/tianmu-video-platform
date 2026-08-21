package com.shanyangcode.tianmu.websocket;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.shanyangcode.tianmu.constants.SnowflakeConstant;
import com.shanyangcode.tianmu.constants.WebSocketConstant;
import com.shanyangcode.tianmu.model.dto.bullet.SendBulletRequest;
import com.shanyangcode.tianmu.model.vo.bullet.BulletScreenResponse;
import com.shanyangcode.tianmu.model.vo.bullet.OnlineBulletResponse;
import com.shanyangcode.tianmu.producer.RocketMQProducer;
import com.shanyangcode.tianmu.service.AuthSessionService;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Slf4j
@AllArgsConstructor
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {


    private final RocketMQProducer producer;

    private final AuthSessionService authSessionService;

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);


    private static final ConcurrentMap<String, ChannelGroup> videoMap = new ConcurrentHashMap<>();

    private static final AttributeKey<String> VIDEOID = AttributeKey.valueOf("videoId");

    private static final AttributeKey<String> VIEWER_ID = AttributeKey.valueOf("viewerId");



    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        if ("ping".equals(msg.text())) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame("pong"));
            return;
        }

        String videoId = ctx.channel().attr(VIDEOID).get();
        if (videoId == null) {
            sendErrorMessage(ctx.channel(), "尚未加入视频房间");
            return;
        }

        final SendBulletRequest request;
        try {
            request = JSONUtil.toBean(msg.text(), SendBulletRequest.class);
        } catch (Exception e) {
            sendErrorMessage(ctx.channel(), "弹幕消息格式错误");
            return;
        }

        if (!checkOnline(request)) {
            needLoginMessage(ctx.channel());
            return;
        }
        if (request.getVideoId() == null || !videoId.equals(request.getVideoId().toString())) {
            sendErrorMessage(ctx.channel(), "视频房间不匹配");
            return;
        }
        String validationMessage = validateBullet(request);
        if (validationMessage != null) {
            sendErrorMessage(ctx.channel(), validationMessage);
            return;
        }

        try {
            broadcastMessage(videoId, onlineMessage(request));
        } catch (Exception e) {
            log.error("发送弹幕失败，videoId={}, userId={}", videoId, request.getUserId(), e);
            sendErrorMessage(ctx.channel(), "弹幕发送失败，请稍后重试");
        }
    }

    private void broadcastMessage(String videoId, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        ChannelGroup group = videoMap.get(videoId);
        if (group != null && !group.isEmpty()) {
            group.writeAndFlush(new TextWebSocketFrame(message)).addListener(future -> {
                if (!future.isSuccess()) {
                    logger.error("消息失败到房间：{}，原因：{}", videoId, future.cause().getMessage());
                    cleanupInvalidChannels(group);
                }
            });
        }
    }

    public String onlineMessage(SendBulletRequest sendBulletRequest) {
        BulletScreenResponse bulletScreenResponse = new BulletScreenResponse();
        bulletScreenResponse.setType(WebSocketConstant.ONLINE_BULLET);
        Snowflake snowflake = IdUtil.getSnowflake(SnowflakeConstant.WORKER_ID, SnowflakeConstant.DATA_CENTER_ID);
        sendBulletRequest.setBulletId(snowflake.nextId());
        sendBulletRequest.setContent(sendBulletRequest.getContent().trim());
        sendBulletRequest.setToken(null);
        String messageMQ = JSONUtil.toJsonStr(sendBulletRequest);

        producer.sendMessage("zzz-topic", messageMQ);

        OnlineBulletResponse onlineBulletResponse = new OnlineBulletResponse();
        onlineBulletResponse.setPlaybackTime(sendBulletRequest.getPlaybackTime());
        onlineBulletResponse.setText(sendBulletRequest.getContent());
        onlineBulletResponse.setUserId(sendBulletRequest.getUserId().toString());
        onlineBulletResponse.setBulletId(sendBulletRequest.getBulletId().toString());
        bulletScreenResponse.setData(onlineBulletResponse);
        return JSONUtil.parse(bulletScreenResponse).toString();
    }


    private void cleanupInvalidChannels(ChannelGroup group) {
        List<Channel> invalidChannels = group.stream().filter(ch -> !ch.isActive() || !ch.isOpen()).collect(Collectors.toList());
        invalidChannels.forEach(group::remove);
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete handshake) {
            String videoId = extractRoomId(handshake.requestUri());
            if (videoId == null) {
                ctx.close();
                return;
            }
            ctx.channel().attr(VIDEOID).set(videoId);
            ctx.channel().attr(VIEWER_ID).set(extractViewerId(handshake.requestUri(), ctx.channel()));
            joinRoom(videoId, ctx.channel());
            broadcastOnlineCount(videoId);
            return;
        }

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.info("60 秒没有收到客户端心跳，关闭失效连接: {}", ctx.channel());
                ctx.close();
            }
            return;
        }
        super.userEventTriggered(ctx, evt);
    }


    private String extractRoomId(String uri) {
        String path = new QueryStringDecoder(uri).path();
        String prefix = "/ws/bulletScreen/";
        if (!path.startsWith(prefix)) {
            return null;
        }
        String videoId = path.substring(prefix.length());
        return videoId.matches("\\d+") ? videoId : null;
    }

    private String extractViewerId(String uri, Channel channel) {
        List<String> viewerIds = new QueryStringDecoder(uri).parameters().get("viewerId");
        if (viewerIds == null || viewerIds.isEmpty()) {
            return channel.id().asLongText();
        }
        String viewerId = viewerIds.get(0).trim();
        if (viewerId.isEmpty() || viewerId.length() > 128) {
            return channel.id().asLongText();
        }
        return viewerId;
    }

    private void joinRoom(String videoId, Channel channel) {
        videoMap.computeIfAbsent(videoId, k -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)).add(channel);
    }

    private void broadcastOnlineCount(String videoId) {
        ChannelGroup group = videoMap.get(videoId);
        if (group != null) {
            cleanupInvalidChannels(group);
            long uniqueViewerCount = group.stream()
                    .filter(Channel::isActive)
                    .map(channel -> channel.attr(VIEWER_ID).get())
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .count();
            BulletScreenResponse bulletScreenResponse = new BulletScreenResponse();
            bulletScreenResponse.setType(WebSocketConstant.ONLINE_NUMBER);
            bulletScreenResponse.setData(uniqueViewerCount);
            String message = JSONUtil.parse(bulletScreenResponse).toString();
            group.writeAndFlush(new TextWebSocketFrame(message));
        }
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        String videoId = ctx.channel().attr(VIDEOID).get();
        if (videoId != null) {
            ChannelGroup group = videoMap.get(videoId);
            if (group != null) {
                group.remove(ctx.channel());
                if (group.isEmpty()) {
                    videoMap.remove(videoId, group);
                } else {
                    broadcastOnlineCount(videoId);
                }
            }
        }
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("WebSocket 连接异常: {}", ctx.channel(), cause);
        ctx.close();
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    public boolean checkOnline(SendBulletRequest request) {
        if (request.getUserId() == null || StrUtil.isBlank(request.getToken())) {
            return false;
        }
        return authSessionService.isValid(request.getToken(), request.getUserId());
    }

    private String validateBullet(SendBulletRequest request) {
        if (StrUtil.isBlank(request.getContent())) {
            return "弹幕内容不能为空";
        }
        if (request.getContent().trim().length() > 100) {
            return "弹幕不能超过100个字符";
        }
        Double playbackTime = request.getPlaybackTime();
        if (playbackTime == null || playbackTime.isNaN() || playbackTime.isInfinite() || playbackTime < 0) {
            return "弹幕时间不合法";
        }
        return null;
    }

    private void needLoginMessage(Channel channel) {
        BulletScreenResponse bulletScreenResponse = new BulletScreenResponse();
        bulletScreenResponse.setType(WebSocketConstant.LOGIN_MESSAGE);
        bulletScreenResponse.setData("登录状态已失效，请重新登录");
        String message = JSON.toJSONString(bulletScreenResponse);
        channel.writeAndFlush(new TextWebSocketFrame(message)).addListener(future -> {
            if (!future.isSuccess()) {
                logger.error("发送登录提示失败，原因：{}", future.cause().getMessage());
            }
        });
    }

    private void sendErrorMessage(Channel channel, String errorMessage) {
        BulletScreenResponse response = new BulletScreenResponse();
        response.setType(WebSocketConstant.ERROR_MESSAGE);
        response.setData(errorMessage);
        channel.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(response)));
    }

}
