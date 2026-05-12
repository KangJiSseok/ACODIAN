package com.ibank.axwms.domain.file.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibank.axwms.domain.file.repository.jooq.projection.FileSummaryProjection;
import com.ibank.axwms.domain.file.repository.jooq.query.FilePageQuery;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

class FileRepositoryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserTeamRepository userTeamRepository;

    @Autowired
    private WorklogRepository worklogRepository;

    @Autowired
    private FileRepository fileRepository;

    private Long callerUserId;

    @BeforeEach
    void setUpData() {
        clearDatabase();
        seedFiles();
    }

    @Test
    @DisplayName("필터가 없으면 visible scope 의 미삭제 파일을 최신순으로 반환한다")
    void 필터가_없으면_visible_scope의_미삭제_파일을_최신순으로_반환한다() {
        Page<FileSummaryProjection> page = fileRepository.findFilePage(
                callerUserId,
                FilePageQuery.of(1, 20, null, null)
        );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent())
                .extracting(FileSummaryProjection::originalName)
                .containsExactly("visible-recent-png.png", "visible-recent-pdf.pdf", "visible-old-pdf.pdf");
    }

    @Test
    @DisplayName("파일 형식 필터가 있으면 해당 확장자만 반환하고 totalCount 에도 같은 조건을 적용한다")
    void 파일_형식_필터가_있으면_해당_확장자만_반환하고_totalCount에도_같은_조건을_적용한다() {
        Page<FileSummaryProjection> page = fileRepository.findFilePage(
                callerUserId,
                FilePageQuery.of(1, 20, "pdf", null)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(FileSummaryProjection::fileExtension)
                .containsOnly("pdf");
    }

    @Test
    @DisplayName("파일 형식과 기간 필터가 함께 있으면 AND 조건으로 반환한다")
    void 파일_형식과_기간_필터가_함께_있으면_and_조건으로_반환한다() {
        LocalDateTime createdFrom = LocalDateTime.now().minusDays(30);

        Page<FileSummaryProjection> page = fileRepository.findFilePage(
                callerUserId,
                FilePageQuery.of(1, 20, "pdf", createdFrom)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent())
                .singleElement()
                .extracting(FileSummaryProjection::originalName)
                .isEqualTo("visible-recent-pdf.pdf");
    }

    /** 다른 테스트와 FK 순서가 충돌하지 않도록 관련 테이블을 한 번에 비운다. */
    private void clearDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    tb_file_embedding,
                    tb_worklog_embedding,
                    tb_worklog_tag,
                    tb_worklog_dependency,
                    tb_worklog_status_history,
                    tb_file,
                    tb_worklog,
                    tb_team_admin,
                    tb_user_team,
                    tb_team,
                    tb_user,
                    tb_department
                RESTART IDENTITY CASCADE
                """);
    }

    /** 파일 필터와 visible scope 조건을 동시에 검증할 수 있는 최소 데이터만 구성한다. */
    private void seedFiles() {
        Department department = departmentRepository.save(Department.create("파일필터부서", "파일 필터 테스트 부서"));
        User caller = userRepository.save(createUser(department.getId(), "호출자", "file-filter-caller", UserRole.MEMBER));
        User deniedUser = userRepository.save(createUser(department.getId(), "비가시사용자", "file-filter-denied", UserRole.MEMBER));
        callerUserId = caller.getId();

        Team visibleTeam = teamRepository.save(createTeam(department.getId(), "가시파일팀"));
        Team deniedTeam = teamRepository.save(createTeam(department.getId(), "비가시파일팀"));
        userTeamRepository.save(UserTeam.create(caller.getId(), visibleTeam.getId(), true, "담당", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.save(UserTeam.create(deniedUser.getId(), deniedTeam.getId(), true, "담당", "주담당", true, UserTeamStatus.ACTIVE));
        userTeamRepository.flush();

        Worklog visibleWorklog = worklogRepository.save(createWorklog(caller.getId(), visibleTeam.getId(), "가시업무"));
        Worklog deniedWorklog = worklogRepository.save(createWorklog(deniedUser.getId(), deniedTeam.getId(), "비가시업무"));
        worklogRepository.flush();

        insertFile(visibleWorklog.getId(), caller.getId(), "visible-recent-pdf.pdf", "pdf", LocalDateTime.now().minusDays(3), false);
        insertFile(visibleWorklog.getId(), caller.getId(), "visible-recent-png.png", "png", LocalDateTime.now().minusDays(2), false);
        insertFile(visibleWorklog.getId(), caller.getId(), "visible-old-pdf.pdf", "pdf", LocalDateTime.now().minusDays(45), false);
        insertFile(visibleWorklog.getId(), caller.getId(), "visible-deleted-pdf.pdf", "pdf", LocalDateTime.now().minusDays(1), true);
        insertFile(deniedWorklog.getId(), deniedUser.getId(), "denied-recent-pdf.pdf", "pdf", LocalDateTime.now().minusDays(1), false);
    }

    /** 테스트용 사용자를 역할과 식별 가능한 이메일 prefix 로 생성한다. */
    private User createUser(Long departmentId, String userName, String emailPrefix, UserRole role) {
        return User.create(
                departmentId,
                userName,
                emailPrefix + "-" + System.nanoTime() + "@ibank.com",
                "$2a$10$abcdefghijklmnopqrstuv",
                role,
                EmploymentStatus.ACTIVE,
                "사원",
                null,
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
    }

    /** 테스트용 활성 팀을 생성한다. */
    private Team createTeam(Long departmentId, String teamName) {
        return Team.create(
                departmentId,
                teamName,
                TeamStatus.ACTIVE,
                teamName + " 설명",
                LocalDate.of(2026, 4, 1),
                null
        );
    }

    /** 파일 목록 projection 에 필요한 업무 요약 필드를 가진 테스트 업무를 생성한다. */
    private Worklog createWorklog(Long authorId, Long teamId, String title) {
        return Worklog.create(
                authorId,
                teamId,
                title,
                "요청 내용",
                "수행 내용",
                WorklogStatus.IN_PROGRESS,
                WorklogImportance.NORMAL,
                BigDecimal.ONE,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(7)
        );
    }

    /** JPA audit 시각에 묶이지 않고 기간 필터 경계를 검증하기 위해 파일 행을 직접 삽입한다. */
    private void insertFile(Long worklogId,
                            Long uploadedBy,
                            String originalName,
                            String fileExtension,
                            LocalDateTime createdAt,
                            boolean deleted) {
        jdbcTemplate.update("""
                        INSERT INTO tb_file (
                            worklog_id,
                            uploaded_by,
                            original_name,
                            stored_path,
                            file_extension,
                            file_size_bytes,
                            ai_summary,
                            ai_processing_status,
                            is_deleted,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                worklogId,
                uploadedBy,
                originalName,
                "test/" + originalName,
                fileExtension,
                1024L,
                "파일 요약",
                "COMPLETED",
                deleted,
                createdAt,
                createdAt
        );
    }
}
