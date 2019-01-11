package com.redant.cluster.slave;

import com.redant.core.DataHolder;
import com.redant.core.common.exception.InvocationException;
import com.redant.core.common.util.HttpRenderUtil;
import com.redant.core.invocation.ControllerProxy;
import com.redant.core.invocation.ProxyInvocation;
import com.redant.core.render.RenderType;
import com.redant.core.router.RouteResult;
import com.redant.core.router.RouterContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SlaveServerHandler
 * @author gris.wang
 * @date 2017/11/22
 */
public class SlaveServerHandler extends SimpleChannelInboundHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(SlaveServerHandler.class);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        FullHttpResponse response = HttpRenderUtil.render(null,RenderType.TEXT);
        if(msg instanceof HttpRequest){
            HttpRequest request = (HttpRequest) msg;
            DataHolder.store(DataHolder.HolderType.REQUEST,request);
            try{
                // 获得路由结果
                RouteResult<RenderType> routeResult = RouterContext.getRouteResult(request.method(),request.uri());
                // 根据路由获得具体的ControllerProxy
                ControllerProxy controllerProxy = RouterContext.getControllerProxy(routeResult);
                if(controllerProxy == null){
                    response = HttpRenderUtil.getNotFoundResponse();
                }else {
                    // 每一个Controller的方法返回类型约定为Render的实现类
                    Object result = ProxyInvocation.invoke(controllerProxy);
                    response = HttpRenderUtil.render(result,RenderType.JSON);
                }
            }catch(Exception e){
                LOGGER.error("Slave Server channelRead0 error,cause:",e);
                if(e instanceof IllegalArgumentException || e instanceof InvocationException){
                    response = HttpRenderUtil.getErrorResponse(e.getMessage());
                }else{
                    response = HttpRenderUtil.getServerErrorResponse();
                }
            }
        }
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
        DataHolder.removeAll();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("SlaveServerHandler exceptionCaught,cause:",cause);
        ctx.close();
    }


}