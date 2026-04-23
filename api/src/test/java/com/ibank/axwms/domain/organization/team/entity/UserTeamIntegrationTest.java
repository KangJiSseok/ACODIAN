package com.ibank.axwms.domain.organization.team.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class UserTeamIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserTeamRepository userTeamRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @BeforeEach
    void setUp() {
        userTeamRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("tb_user_team 스키마는 teamLeader boolean 과 역할/배치 컬럼을 생성한다")
    void tb_user_team_스키마는_teamLeader_boolean_과_역할_배치_컬럼을_생성한다() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT column_name,
                       data_type,
                       is_nullable,
                       COALESCE(column_default, '') AS column_default
                FROM information_schema.columns
                WHERE table_name = 'tb_user_team'
                  AND column_name IN ('team_leader', 'team_role', 'allocation')
                """);

        assertThat(rows)
                .extracting(row -> row.get("column_name"))
                .containsExactlyInAnyOrder("team_leader", "team_role", "allocation");

        Map<String, Object> teamLeader = rows.stream()
                .filter(row -> "team_leader".equals(row.get("column_name")))
                .findFirst()
                .orElseThrow();
        Map<String, Object> teamRole = rows.stream()
                .filter(row -> "team_role".equals(row.get("column_name")))
                .findFirst()
                .orElseThrow();
        Map<String, Object> allocation = rows.stream()
                .filter(row -> "allocation".equals(row.get("column_name")))
                .findFirst()
                .orElseThrow();

        assertThat(teamLeader.get("data_type")).isEqualTo("boolean");
        assertThat(teamLeader.get("is_nullable")).isEqualTo("NO");
        assertThat(teamLeader.get("column_default").toString().toLowerCase()).contains("false");
        assertThat(teamRole.get("data_type")).isEqualTo("character varying");
        assertThat(teamRole.get("is_nullable")).isEqualTo("NO");
        assertThat(allocation.get("data_type")).isEqualTo("character varying");
        assertThat(allocation.get("is_nullable")).isEqualTo("YES");
    }

    @Test
    @DisplayName("UserTeam 엔티티를 저장하면 teamLeader 와 역할/배치 값이 보존된다")
    void UserTeam_엔티티를_저장하면_teamLeader_와_역할_배치_값이_보존된다() {
        Department department = departmentRepository.save(Department.create("통합테스트부서", "통합 테스트 부서"));
        Team team = teamRepository.save(Team.create(
                department.getId(),
                "통합테스트팀",
                TeamStatus.ACTIVE,
                "통합 테스트 팀",
                LocalDate.of(2025, 1, 1),
                null
        ));
        User user = userRepository.save(User.create(
                department.getId(),
                "통합 테스트 사용자",
                "integration-user@ibank.com",
                "$2a$10$integration-hashed",
                UserRole.MEMBER,
                EmploymentStatus.ACTIVE,
                "대리",
                "팀원",
                LocalDate.of(2025, 1, 1)
        ));

        UserTeam saved = userTeamRepository.saveAndFlush(UserTeam.create(
                user.getId(),
                team.getId(),
                true,
                "플랫폼 총괄",
                "주담당",
                true
        ));

        UserTeam found = userTeamRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTeamLeader()).isTrue();
        assertThat(found.getTeamRole()).isEqualTo("플랫폼 총괄");
        assertThat(found.getAllocation()).isEqualTo("주담당");
        assertThat(found.getIsPrimary()).isTrue();
    }
}
