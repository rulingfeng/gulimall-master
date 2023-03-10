package com.vivi.gulimall.order.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author  
 * 2021/1/12 17:31
 */
@ConfigurationProperties(prefix = "thread.pool.config")
@Component
@Data
@ToString
public class ThreadPoolProperties {

    private int coreSize;

    private int maximumSize;

    private long keepAliveTime;

    private int blockQueueSize;
}
