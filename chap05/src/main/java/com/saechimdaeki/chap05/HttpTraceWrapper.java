package com.saechimdaeki.chap05;

import lombok.Getter;
import org.springframework.boot.actuate.trace.http.HttpTrace;
import org.springframework.data.annotation.Id;

@Getter
public class HttpTraceWrapper {
    private @Id String id;
    private HttpTrace httpTrace;

    public HttpTraceWrapper(HttpTrace httpTrace) {
        this.httpTrace = httpTrace;
    }
}
