package com.ibank.axwms.testsupport;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles(value = {"integration", "e2e"}, inheritProfiles = false)
public abstract class E2eTestSupport extends IntegrationTestSupport {

    private static final String API_CONTEXT_PATH = "/api";

    @Autowired
    private WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;

    @BeforeEach
    void initMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    /** server.servlet.context-path 를 반영한 GET 요청 빌더를 만든다. */
    protected MockHttpServletRequestBuilder apiGet(String path) {
        return get(withContextPath(path))
                .contextPath(API_CONTEXT_PATH);
    }

    /** server.servlet.context-path 를 반영한 POST 요청 빌더를 만든다. */
    protected MockHttpServletRequestBuilder apiPost(String path) {
        return post(withContextPath(path))
                .contextPath(API_CONTEXT_PATH);
    }

    private String withContextPath(String path) {
        return API_CONTEXT_PATH + path;
    }
}
