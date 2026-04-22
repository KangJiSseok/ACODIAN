package com.ibank.axwms.devsupport.bootstrap;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.TeamRole;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * local 프로파일 전용 시드 러너.
 * bootRun + Swagger 수동 검증을 위해 최소 부서/사용자 집합을 멱등 삽입한다.
 * 비밀번호는 PasswordEncoder 로 런타임 해시하고, Repository 의 existsBy/findBy 파생 쿼리로
 * 중복 삽입을 회피하므로 재기동 시에도 UNIQUE 충돌을 일으키지 않는다.
 * 운영/CI 환경에서는 빈이 생성되지 않도록 반드시 다른 프로파일로 기동한다.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class LocalSeedRunner implements ApplicationRunner {

    private static final String DEV_PASSWORD = "password1!";
    private static final LocalDate DEFAULT_JOIN_DATE = LocalDate.of(2024, 1, 1);

    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final UserTeamRepository userTeamRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 기동 직후 부서 → 사용자 순으로 시드를 삽입한다.
     * FK(tb_user.department_id → tb_department) 무결성을 위해 부서 저장으로 id 를 확보한 뒤 사용자를 만든다.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long devDepartmentId = ensureDepartment("개발본부", "개발 전반을 담당하는 본부");
        Long salesDepartmentId = ensureDepartment("영업본부", "영업 전반을 담당하는 본부");

        Long directorUserId = ensureUser("director@ibank.com", devDepartmentId, UserRole.DIRECTOR, "김이사", "이사", "본부장");
        Long deptHeadUserId = ensureUser("dept@ibank.com", devDepartmentId, UserRole.DEPT_HEAD, "박부장", "부장", "부서장");
        Long memberUserId = ensureUser("member@ibank.com", salesDepartmentId, UserRole.MEMBER, "이사원", "사원", "팀원");

        Long platformTeamId = ensureTeam(devDepartmentId, "플랫폼개발팀", "로컬 검증용 개발본부 팀");
        Long architectureTeamId = ensureTeam(devDepartmentId, "아키텍처TF", "로컬 검증용 개발본부 추가 팀");
        Long salesTeamId = ensureTeam(salesDepartmentId, "영업운영팀", "로컬 검증용 영업본부 팀");
        Long salesStrategyTeamId = ensureTeam(salesDepartmentId, "영업전략TF", "로컬 검증용 영업본부 추가 팀");

        ensureUserTeam(directorUserId, platformTeamId, TeamRole.LEADER, true);
        ensureUserTeam(directorUserId, architectureTeamId, TeamRole.MEMBER, false);
        ensureUserTeam(deptHeadUserId, platformTeamId, TeamRole.MEMBER, true);
        ensureUserTeam(deptHeadUserId, architectureTeamId, TeamRole.LEADER, false);
        ensureUserTeam(memberUserId, salesTeamId, TeamRole.MEMBER, true);
        ensureUserTeam(memberUserId, salesStrategyTeamId, TeamRole.MEMBER, false);
    }

    /**
     * 주어진 이름의 부서가 없으면 새로 저장하고, 있으면 기존 id 를 반환한다.
     * department_name UNIQUE 제약이 있어 멱등성을 보장한다.
     */
    private Long ensureDepartment(String name, String description) {
        return departmentRepository.findByDepartmentName(name)
                .map(Department::getId)
                .orElseGet(() -> {
                    Department saved = departmentRepository.save(Department.create(name, description));
                    log.info("[LocalSeed] 부서 생성 - name={} id={}", name, saved.getId());
                    return saved.getId();
                });
    }

    /**
     * 주어진 이메일의 사용자가 없으면 새로 저장한다. 이미 있으면 skip.
     * 비밀번호는 DEV_PASSWORD 를 BCrypt 로 해시해 저장한다.
     */
    private Long ensureUser(String email,
                            Long departmentId,
                            UserRole role,
                            String userName,
                            String positionName,
                            String titleName) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.synchronizeSeedProfile(
                            departmentId,
                            userName,
                            role,
                            EmploymentStatus.ACTIVE,
                            positionName,
                            titleName,
                            DEFAULT_JOIN_DATE
                    );
                    log.info("[LocalSeed] 사용자 보정 - email={} role={} departmentId={} titleName={}",
                            email, role, departmentId, titleName);
                    return user.getId();
                })
                .orElseGet(() -> {
                    User user = User.create(
                            departmentId,
                            userName,
                            email,
                            passwordEncoder.encode(DEV_PASSWORD),
                            role,
                            EmploymentStatus.ACTIVE,
                            positionName,
                            titleName,
                            DEFAULT_JOIN_DATE
                    );
                    User saved = userRepository.save(user);
                    log.info("[LocalSeed] 사용자 생성 - email={} role={} departmentId={} titleName={}",
                            email, role, departmentId, titleName);
                    return saved.getId();
                });
    }

    /**
     * 부서 안의 로컬 검증용 팀이 없으면 생성한다.
     * /api/users/me 수동 검증 시 teams 응답이 비지 않도록 최소 팀 데이터를 함께 유지한다.
     */
    private Long ensureTeam(Long departmentId, String teamName, String description) {
        return teamRepository.findByDepartmentIdAndTeamName(departmentId, teamName)
                .map(Team::getId)
                .orElseGet(() -> {
                    Team saved = teamRepository.save(Team.create(
                            departmentId,
                            teamName,
                            TeamStatus.ACTIVE,
                            description,
                            DEFAULT_JOIN_DATE,
                            null
                    ));
                    log.info("[LocalSeed] 팀 생성 - departmentId={} teamName={} teamId={}", departmentId, teamName, saved.getId());
                    return saved.getId();
                });
    }

    /**
     * 사용자-팀 관계가 없으면 생성한다.
     * 로컬 로그인 후 현재 사용자 조회 API 가 항상 팀 컨텍스트를 돌려줄 수 있도록 최소 한 건의 소속을 보장한다.
     */
    private void ensureUserTeam(Long userId, Long teamId, TeamRole teamRole, boolean isPrimary) {
        if (userTeamRepository.findByUserIdAndTeamId(userId, teamId).isPresent()) {
            log.info("[LocalSeed] 사용자-팀 관계 skip - userId={} teamId={} (이미 존재)", userId, teamId);
            return;
        }
        userTeamRepository.save(UserTeam.create(userId, teamId, teamRole, isPrimary));
        log.info("[LocalSeed] 사용자-팀 관계 생성 - userId={} teamId={} primary={}", userId, teamId, isPrimary);
    }
}
