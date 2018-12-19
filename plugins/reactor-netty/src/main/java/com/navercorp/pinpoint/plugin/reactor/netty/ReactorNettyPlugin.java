package com.navercorp.pinpoint.plugin.reactor.netty;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.instrument.*;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.MatchableTransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.MatchableTransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

import java.security.ProtectionDomain;

/**
 * Created by linxiao on 2018/12/17.
 */
public class ReactorNettyPlugin implements ProfilerPlugin, MatchableTransformTemplateAware {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isInfo = logger.isInfoEnabled();

    private MatchableTransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        final ReactorNettyConfig config = new ReactorNettyConfig(context.getConfig());
        if (logger.isInfoEnabled()) {
            logger.info("ReactorNettyConfig config:{}", config);
        }

        if (!config.isEnable() || (!config.isEnableHttpServer() )) {
            if (isInfo) {
                logger.info("Disable ReactorNettyPlugin.");
            }
            return;
        }

        final ReactorNettyDetector detector = new ReactorNettyDetector(config.getBootstrapMains());
        context.addApplicationTypeDetector(detector);

        if (config.isEnableHttpServer()) {
            if (isInfo) {
                logger.info("Adding ReactorNetty HTTP Server.");
            }
            // Entry Point
            addContextHandlerMethod();
            addHttpServerRequestImpl();
            addHttpServerResponseImpl();
        }
    }

    private void addContextHandlerMethod() {
        transformTemplate.transform("reactor.ipc.netty.channel.ContextHandler", new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
                final InstrumentMethod handleRequestMethod = target.getDeclaredMethod("accept", "io.netty.channel.Channel");
                if (handleRequestMethod != null) {
                    // entry point & set asynchronous of req, res.
                    handleRequestMethod.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.interceptor.ServerContextHandlerInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addHttpServerRequestImpl() {
        transformTemplate.transform("reactor.ipc.netty.http.server.HttpServerHandler", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(AsyncContextAccessor.class.getName());

                final InstrumentMethod handleExceptionMethod = target.getDeclaredMethod("channelRead", "io.netty.channel.ChannelHandlerContext","java.lang.Object");
                if (handleExceptionMethod != null) {
                    handleExceptionMethod.addInterceptor("com.navercorp.pinpoint.plugin.reactor.netty.interceptor.HttpServerHandlerInterceptor");
                }

                return target.toBytecode();
            }
        });
    }
    private void addHttpServerResponseImpl() {
        transformTemplate.transform("reactor.ipc.netty.http.server.HttpServerOperations", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(AsyncContextAccessor.class.getName());

                final InstrumentMethod endBufferMethod = target.getDeclaredMethod("status");
                if (endBufferMethod != null) {
                    endBufferMethod.addInterceptor("com.navercorp.pinpoint.plugin.vertx.interceptor.HttpServerResponseImplInterceptor");
                }

                return target.toBytecode();
            }
        });
    }



    @Override
    public void setTransformTemplate(MatchableTransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }


}
