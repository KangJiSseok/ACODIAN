package com.ibank.axwms.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.ibank.axwms.domain.notification.entity.Notification;
import com.ibank.axwms.domain.notification.repository.jooq.projection.NotificationSearchProjection;
import com.ibank.axwms.domain.notification.repository.jooq.query.NotificationSearchQuery;
import com.ibank.axwms.domain.notification.dto.SearchNotificationsApiDto;
import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class NotificationRepositoryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clearDatabase();
    }

    @Test
    @DisplayName("내 알림 목록은 수신자 기준으로 조회하고 읽음 상태 null 이면 전체를 반환한다")
    void 내_알림_목록은_수신자_기준으로_조회하고_읽음_상태_null이면_전체를_반환한다() {
        Fixture fixture = seedFixture();

        Page<NotificationSearchProjection> result = notificationRepository.searchNotifications(
                fixture.callerId(),
                NotificationSearchQuery.from(new SearchNotificationsApiDto.Request(null, null, null, 1, 20))
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(
                        NotificationSearchProjection::title,
                        NotificationSearchProjection::isRead
                )
                .containsExactly(
                        tuple("읽은 알림", true),
                        tuple("안읽은 알림", false)
                );
    }

    @Test
    @DisplayName("내 알림 목록은 읽음 상태와 부서 팀 필터를 함께 적용한다")
    void 내_알림_목록은_읽음_상태와_부서_팀_필터를_함께_적용한다() {
        Fixture fixture = seedFixture();

        Page<NotificationSearchProjection> result = notificationRepository.searchNotifications(
                fixture.callerId(),
                NotificationSearchQuery.from(new SearchNotificationsApiDto.Request(false, fixture.departmentId(), fixture.teamId(), 1, 20))
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).singleElement()
                .extracting(
                        NotificationSearchProjection::title,
                        NotificationSearchProjection::departmentId,
                        NotificationSearchProjection::teamId,
                        NotificationSearchProjection::isRead
                )
                .containsExactly("안읽은 알림", fixture.departmentId(), fixture.teamId(), false);
    }

    @Test
    @DisplayName("내 알림 목록은 updated_at 최신순과 notification_id 역순으로 정렬한다")
    void 내_알림_목록은_updated_at_최신순과_notification_id_역순으로_정렬한다() {
        // given
        Department department = departmentRepository.save(createDepartment("정렬검증본부"));
        User caller = userRepository.save(createUser(department.getId(), "정렬호출자", UserRole.MEMBER));
        Team team = teamRepository.save(createTeam(department.getId(), "정렬검증팀"));
        Notification oldestCreatedButNewestUpdated = notificationRepository.saveAndFlush(notification(
                caller.getId(),
                department.getId(),
                team.getId(),
                "생성은 오래됐지만 갱신 최신",
                false
        ));
        Notification sameUpdatedLowId = notificationRepository.saveAndFlush(notification(
                caller.getId(),
                department.getId(),
                team.getId(),
                "같은 갱신시각 낮은 ID",
                false
        ));
        Notification sameUpdatedHighId = notificationRepository.saveAndFlush(notification(
                caller.getId(),
                department.getId(),
                team.getId(),
                "같은 갱신시각 높은 ID",
                false
        ));
        forceNotificationUpdatedAt(oldestCreatedButNewestUpdated.getId(), LocalDateTime.of(2026, 5, 12, 10, 0));
        forceNotificationUpdatedAt(sameUpdatedLowId.getId(), LocalDateTime.of(2026, 5, 12, 9, 0));
        forceNotificationUpdatedAt(sameUpdatedHighId.getId(), LocalDateTime.of(2026, 5, 12, 9, 0));

        // when
        Page<NotificationSearchProjection> result = notificationRepository.searchNotifications(
                caller.getId(),
                NotificationSearchQuery.from(new SearchNotificationsApiDto.Request(null, null, null, 1, 20))
        );

        // then
        assertThat(result.getContent())
                .extracting(NotificationSearchProjection::title)
                .containsExactly(
                        "생성은 오래됐지만 갱신 최신",
                        "같은 갱신시각 높은 ID",
                        "같은 갱신시각 낮은 ID"
                );
    }

    @Test
    @Transactional
    @DisplayName("내 알림 전체 읽음 처리는 JPA 로 내 안읽은 알림만 읽음 처리한다")
    void 내_알림_전체_읽음_처리는_JPA로_내_안읽은_알림만_읽음_처리한다() {
        Fixture fixture = seedFixture();
        LocalDateTime readAt = LocalDateTime.of(2026, 5, 11, 11, 0);

        int updatedCount = notificationRepository.markUnreadAsReadByUserId(fixture.callerId(), readAt);

        assertThat(updatedCount).isEqualTo(1);
        assertThat(notificationRepository.findAll())
                .extracting(Notification::getTitle, Notification::getIsRead, Notification::getReadAt)
                .contains(
                        tuple("안읽은 알림", true, readAt),
                        tuple("읽은 알림", true, LocalDateTime.of(2026, 5, 11, 10, 0)),
                        tuple("다른 사용자 알림", false, null)
                );
    }

    private void clearDatabase() {
        notificationRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    private Fixture seedFixture() {
        Department department = departmentRepository.save(createDepartment("알림검증본부"));
        Department otherDepartment = departmentRepository.save(createDepartment("다른본부"));
        User caller = userRepository.save(createUser(department.getId(), "호출자", UserRole.MEMBER));
        User otherUser = userRepository.save(createUser(otherDepartment.getId(), "다른사용자", UserRole.MEMBER));
        Team team = teamRepository.save(createTeam(department.getId(), "알림검증팀"));
        Team otherTeam = teamRepository.save(createTeam(otherDepartment.getId(), "다른팀"));

        notificationRepository.save(notification(
                caller.getId(),
                department.getId(),
                team.getId(),
                "안읽은 알림",
                false
        ));
        notificationRepository.save(notification(
                caller.getId(),
                otherDepartment.getId(),
                otherTeam.getId(),
                "읽은 알림",
                true
        ));
        notificationRepository.save(notification(
                otherUser.getId(),
                department.getId(),
                team.getId(),
                "다른 사용자 알림",
                false
        ));
        notificationRepository.flush();

        return new Fixture(caller.getId(), department.getId(), team.getId());
    }

    private Department createDepartment(String departmentName) {
        Department department = Department.create(departmentName, departmentName + " 설명");
        department.changeStatus(DepartmentStatus.ACTIVE);
        return department;
    }

    private User createUser(Long departmentId, String userName, UserRole role) {
        String unique = userName + "-" + System.nanoTime() + "@ibank.com";
        return User.create(
                departmentId,
                userName,
                unique,
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

    private Team createTeam(Long departmentId, String teamName) {
        return Team.create(
                departmentId,
                teamName,
                TeamStatus.ACTIVE,
                teamName + " 설명",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31)
        );
    }

    private Notification notification(Long userId, Long departmentId, Long teamId, String title, boolean isRead) {
        return Notification.create(
                userId,
                departmentId,
                teamId,
                "WORKLOG_CREATED",
                title,
                title + " 본문",
                "WORKLOG",
                501L,
                isRead,
                isRead ? LocalDateTime.of(2026, 5, 11, 10, 0) : null
        );
    }

    /**
     * 목록 정렬 기준이 생성시각이 아니라 갱신시각임을 고정하기 위해 DB 감사 필드를 직접 조정한다.
     */
    private void forceNotificationUpdatedAt(Long notificationId, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "UPDATE tb_notification SET updated_at = ? WHERE notification_id = ?",
                updatedAt,
                notificationId
        );
    }

    private record Fixture(Long callerId, Long departmentId, Long teamId) {
    }
}
