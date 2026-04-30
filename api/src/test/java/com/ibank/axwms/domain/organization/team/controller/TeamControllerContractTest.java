package com.ibank.axwms.domain.organization.team.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class TeamControllerContractTest {

    @Test
    @DisplayName("TeamController 는 /teams 리소스 경로만 노출한다")
    void TeamController_는_teams_리소스_경로만_노출한다() {
        assertThat(TeamController.class.getAnnotation(RestController.class)).isNotNull();

        RequestMapping requestMapping = TeamController.class.getAnnotation(RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("/teams");
        assertThat(TeamController.class.getDeclaredMethods()).isEmpty();
    }

    @Test
    @DisplayName("TeamControllerDocs 는 Team Swagger 태그만 유지한다")
    void TeamControllerDocs_는_Team_Swagger_태그만_유지한다() {
        Tag tag = TeamControllerDocs.class.getAnnotation(Tag.class);

        assertThat(tag).isNotNull();
        assertThat(tag.name()).isEqualTo("Team");
        assertThat(TeamControllerDocs.class.getDeclaredMethods()).isEmpty();
    }
}
