package com.ibank.axwms.domain.file.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibank.axwms.testsupport.E2eTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileControllerE2eTest extends E2eTestSupport {

    @Test
    @DisplayName("허용되지 않은 파일 형식 필터 값이면 400 응답을 반환한다")
    void 허용되지_않은_파일_형식_필터_값이면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/files")
                        .param("fileType", "TXT")
                        .with(user("member@ibank.com").roles("MEMBER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.statusCode", is(400)));
    }

}
