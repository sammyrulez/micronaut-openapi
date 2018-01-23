/*
 * Copyright 2017 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.particleframework.http.server.netty.async;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;

/**
 * A future that executes the standard close procedure
 *
 * @author James Kleeh
 * @since 1.0
 */
public class DefaultCloseHandler implements GenericFutureListener<ChannelFuture> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCloseHandler.class);


    private final ChannelHandlerContext context;
    private final HttpRequest request;
    private final HttpResponse response;

    public DefaultCloseHandler(ChannelHandlerContext context, HttpRequest request, HttpResponse response) {
        this.context = context;
        this.request = request;
        this.response = response;
    }

    @Override
    public void operationComplete(ChannelFuture future) {
        if (!future.isSuccess()) {
            Throwable cause = future.cause();
            // swallow closed channel exception, nothing we can do about it if the client disconnects
            if (!(cause instanceof ClosedChannelException)) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Error writing Netty response: " + cause.getMessage(), cause);
                }
                Channel channel = context.channel();
                if (channel.isWritable()) {
                    context.pipeline().fireExceptionCaught(cause);
                } else {
                    channel.close();
                }
            }
        } else if (!HttpUtil.isKeepAlive(request) || response.status().code() >= 300) {
            future.channel().close();
        }
    }
}