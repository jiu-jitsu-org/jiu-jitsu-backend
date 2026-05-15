package com.jiujitsu.api.global.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;

public class P6spySqlFormatter implements MessageFormattingStrategy {

    @Override
    public String formatMessage(
            int connectionId,
            String now,
            long elapsed,
            String category,
            String prepared,
            String sql,
            String url
    ) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        return "\n" +
                "Execution Time: " + elapsed + " ms\n" +
                FormatStyle.BASIC.getFormatter().format(sql) +
                "\n";
    }
}
