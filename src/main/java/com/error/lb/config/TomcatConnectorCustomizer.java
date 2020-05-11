package com.error.lb.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class TomcatConnectorCustomizer implements org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer {

    @Value("${application.custom.keepAliveTimeout:#{null}}")
    private Integer keepAliveTimeout;
    @Value("${application.custom.connectionTimeout:#{null}}")
    private Integer connectionTimeout;
    @Value("${application.custom.maxKeepAliveRequests:#{null}}")
    private Integer maxKeepAliveRequests;
    @Value("${application.shutdown.executor.waitTimeInSecs:10}")
    private int executorWaitTime;
    @Autowired
    private ConfigurableApplicationContext applicationContext;
    private Connector connector;
    private static final Logger log = LoggerFactory.getLogger(TomcatConnectorCustomizer.class);

    @Override
    public void customize(Connector connector) {
        this.connector = connector;
        AbstractHttp11Protocol protocol = (AbstractHttp11Protocol) connector.getProtocolHandler();
        if (Objects.nonNull(keepAliveTimeout))
            protocol.setKeepAliveTimeout(keepAliveTimeout);
        if (Objects.nonNull(maxKeepAliveRequests))
            protocol.setMaxKeepAliveRequests(maxKeepAliveRequests);
        if (Objects.nonNull(connectionTimeout))
            protocol.setConnectionTimeout(connectionTimeout);
        log.info(
                "####################################################################################");
        log.info("#");
        log.info("# SPETomcatCustomizer");
        log.info("#");
        log.info("# custom maxKeepAliveRequests {}", protocol.getMaxKeepAliveRequests());
        log.info("# keepAlive timeout: {} ms", protocol.getKeepAliveTimeout());
        log.info("# connection timeout: {} ms", protocol.getConnectionTimeout());
        log.info("# max connections: {}", protocol.getMaxConnections());
        log.info("# max Threads connections: {}", protocol.getMaxThreads());
        log.info("#");
        log.info(
                "####################################################################################");
    }

    //Based on https://github.com/spring-projects/spring-boot/issues/4657
    @EventListener(ContextClosedEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public void contextClosed(ContextClosedEvent event) throws InterruptedException {
        log.info("Application : {} is running ? : {}", applicationContext.getEnvironment().getProperty("spring.application.name"), applicationContext.isRunning());
        if (connector == null) {
            return;
        }
        if (isCloseEventForApplication(event)) {
            stopAcceptingNewRequests();
            shutdownThreadPoolExecutor();
        }
    }

    private boolean isCloseEventForApplication(ContextClosedEvent event) {
        return event.getApplicationContext().equals(applicationContext);
    }

    private void stopAcceptingNewRequests() {
        connector.pause();
        connector.getProtocolHandler().closeServerSocketGraceful();
        log.info("Paused {} to stop accepting new requests", connector);
    }

    private void shutdownThreadPoolExecutor() throws InterruptedException {
        ThreadPoolExecutor executor = getThreadPoolExecutor();
        if (executor != null) {
            log.info("Initiating shutdown for the executor service, Active thread count is {}", executor.getActiveCount());
            executor.shutdown();
            if (executor.getActiveCount() > 0) {
                log.info("Tomcat threads : {} are active, so waiting for {}", executor.getActiveCount(), executorWaitTime);
                awaitTermination(executor);
            }
            log.info("Tomcat thread pool is empty, we stop now");
        }
    }

    private ThreadPoolExecutor getThreadPoolExecutor() {
        Executor executor = connector.getProtocolHandler().getExecutor();
        if (executor instanceof ThreadPoolExecutor) {
            return (ThreadPoolExecutor) executor;
        }
        return null;
    }

    private void awaitTermination(ThreadPoolExecutor executor) throws InterruptedException {
        if (executor.awaitTermination(executorWaitTime, TimeUnit.SECONDS)) {
            log.warn("{} thread(s) still active after delay of {}, force shutdown", executor.getActiveCount(), executorWaitTime);
        }
    }
}
