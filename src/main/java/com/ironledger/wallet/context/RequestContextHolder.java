package com.ironledger.wallet.context;

public class RequestContextHolder {
    private static final ThreadLocal<String> ipHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> uaHolder = new ThreadLocal<>();

    public static void set(String ip, String userAgent) {
        ipHolder.set(ip);
        uaHolder.set(userAgent);
    }

    public static String getIp() {
        return ipHolder.get();
    }

    public static String getUserAgent() {
        return uaHolder.get();
    }

    public static void clear() {
        ipHolder.remove();
        uaHolder.remove();
    }
}
