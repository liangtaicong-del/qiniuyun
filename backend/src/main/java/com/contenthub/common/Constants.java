package com.contenthub.common;

public class Constants {

    public enum Platform {
        WECHAT("微信公众号"),
        WEIBO("微博"),
        ZHIHU("知乎"),
        JIANSHU("简书"),
        CSDN("CSDN"),
        JUEJIN("掘金"),
        BAIJIA("百家号"),
        TOUTIAO("头条号");

        private final String name;

        Platform(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static final String JWT_PREFIX = "Bearer ";
}
