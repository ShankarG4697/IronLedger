package com.ironledger.wallet.utils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MetaBuilder {
    public static Map<String, Object> buildBaseMeta(String action, String ip, String userAgent) {
        Map<String, Object> meta = new HashMap<>();

        meta.put("action", action);           // CREDIT / DEBIT / CAPTURE / RELEASE
        meta.put("trace_id", UUID.randomUUID().toString());
        meta.put("source", "WALLET_API");
        meta.put("timestamp", OffsetDateTime.now().toString());

        if (ip != null) meta.put("ip_address", ip);
        if (userAgent != null) meta.put("user_agent", userAgent);

        return meta;
    }
}
