package com.smartstay.hotel.config;

import com.smartstay.hotel.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultStatusAdviceTest {

    private final ResultStatusAdvice advice = new ResultStatusAdvice();

    @Test
    void mapsErrorCodeToHttpStatus() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        advice.beforeBodyWrite(Result.error(404, "不存在"), null,
                MediaType.APPLICATION_JSON, null, null,
                new ServletServerHttpResponse(servletResponse));

        assertEquals(404, servletResponse.getStatus());
    }

    @Test
    void leavesSuccessfulResponseAsHttp200() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        advice.beforeBodyWrite(Result.success(), null,
                MediaType.APPLICATION_JSON, null, null,
                new ServletServerHttpResponse(servletResponse));

        assertEquals(200, servletResponse.getStatus());
    }
}
