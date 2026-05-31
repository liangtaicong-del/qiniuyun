package com.contenthub.platform;

import com.contenthub.common.Constants;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 平台发布器注册中心
 * 维护平台枚举与具体发布器的映射关系
 */
@Component
public class PlatformPublisherRegistry {

    private final Map<Constants.Platform, PlatformPublisher> publishers;

    public PlatformPublisherRegistry(List<PlatformPublisher> publisherList) {
        this.publishers = new EnumMap<>(Constants.Platform.class);
        for (PlatformPublisher publisher : publisherList) {
            String name = publisher.getClass().getSimpleName()
                    .replace("Publisher", "")
                    .replace("Publisher", "")
                    .toUpperCase();

            try {
                Constants.Platform platform = Constants.Platform.valueOf(name);
                publishers.put(platform, publisher);
            } catch (IllegalArgumentException e) {
                // 未匹配到已知平台，跳过
            }
        }
    }

    public PlatformPublisher getPublisher(Constants.Platform platform) {
        return publishers.get(platform);
    }

    public boolean hasPublisher(Constants.Platform platform) {
        return publishers.containsKey(platform);
    }
}
