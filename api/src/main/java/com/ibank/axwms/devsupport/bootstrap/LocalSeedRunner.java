package com.ibank.axwms.devsupport.bootstrap;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final List<DepartmentSeedSpec> DEPARTMENT_SEEDS = List.of(
            new DepartmentSeedSpec("솔루션개발사업부", "전사 물류 시스템 개발과 아키텍처를 담당하는 사업부", DepartmentStatus.ACTIVE, "dept@ibank.com"),
            new DepartmentSeedSpec("솔루션사업부", "솔루션 운영 지원과 정산을 담당하는 사업부", DepartmentStatus.ACTIVE, "ops.head@ibank.com"),
            new DepartmentSeedSpec("데이터컨설팅사업부", "삭제 no-op 검증을 위한 비활성 사업부", DepartmentStatus.INACTIVE, "dormant.head@ibank.com"),
            new DepartmentSeedSpec("비상대응본부", "부서장 후보 검증을 위한 사업부", DepartmentStatus.ACTIVE, null)
    );
    private static final List<UserSeedSpec> USER_SEEDS = List.of(
            new UserSeedSpec("director@ibank.com", "솔루션개발사업부", UserRole.DIRECTOR, "김이사", "이사", "본부장", EmploymentStatus.ACTIVE),
            new UserSeedSpec("dept@ibank.com", "솔루션개발사업부", UserRole.DEPT_HEAD, "박본부", "부장", "본부장", EmploymentStatus.ACTIVE),
            new UserSeedSpec("dev.member@ibank.com", "솔루션개발사업부", UserRole.MEMBER, "최개발", "대리", "팀원", EmploymentStatus.ACTIVE),
            new UserSeedSpec("ops.head@ibank.com", "솔루션사업부", UserRole.DEPT_HEAD, "오운영", "부장", "본부장", EmploymentStatus.ACTIVE),
            new UserSeedSpec("dormant.head@ibank.com", "데이터컨설팅사업부", UserRole.DEPT_HEAD, "한휴면", "부장", "본부장", EmploymentStatus.ACTIVE),
            new UserSeedSpec("candidate.head@ibank.com", "비상대응본부", UserRole.DEPT_HEAD, "윤후보", "차장", "부서장 후보", EmploymentStatus.ACTIVE),
            new UserSeedSpec("lead@ibank.com", "솔루션개발사업부", UserRole.MEMBER, "류팀장", "차장", "팀장", EmploymentStatus.ACTIVE),
            new UserSeedSpec("member@ibank.com", "솔루션사업부", UserRole.MEMBER, "이사원", "사원", "팀원", EmploymentStatus.ACTIVE)
    );
    private static final List<TeamSeedSpec> TEAM_SEEDS = List.of(
            new TeamSeedSpec("솔루션개발사업부", "플랫폼개발팀", TeamStatus.ACTIVE, "로컬 검증용 솔루션개발사업부 팀", List.of()),
            new TeamSeedSpec("솔루션개발사업부", "아키텍처TF", TeamStatus.ACTIVE, "로컬 검증용 솔루션개발사업부 추가 팀", List.of()),
            new TeamSeedSpec("솔루션개발사업부", "플랫폼운영TF", TeamStatus.ACTIVE, "로컬 TEAM_LEAD 권한 검증용 팀", List.of()),
            new TeamSeedSpec("솔루션사업부", "운영지원팀", TeamStatus.INACTIVE, "로컬 검증용 솔루션사업부 팀", List.of("영업운영팀")),
            new TeamSeedSpec("솔루션사업부", "운영정산TF", TeamStatus.INACTIVE, "로컬 검증용 솔루션사업부 추가 팀", List.of("영업전략TF"))
    );
    private static final List<UserTeamSeedSpec> USER_TEAM_SEEDS = List.of(
            new UserTeamSeedSpec("director@ibank.com", "솔루션개발사업부", "플랫폼개발팀", true, "플랫폼 총괄", "주담당", true, UserTeamStatus.ACTIVE),
            new UserTeamSeedSpec("director@ibank.com", "솔루션개발사업부", "아키텍처TF", false, "아키텍처 자문", "겸임", false, UserTeamStatus.ACTIVE),
            new UserTeamSeedSpec("dept@ibank.com", "솔루션개발사업부", "플랫폼개발팀", false, "플랫폼 운영", "주담당", true, UserTeamStatus.ACTIVE),
            new UserTeamSeedSpec("dept@ibank.com", "솔루션개발사업부", "아키텍처TF", true, "아키텍처 리드", "겸임", false, UserTeamStatus.ACTIVE),
            new UserTeamSeedSpec("dev.member@ibank.com", "솔루션개발사업부", "플랫폼개발팀", false, "WMS 개발", "겸임", false, UserTeamStatus.ACTIVE),
            new UserTeamSeedSpec("lead@ibank.com", "솔루션개발사업부", "플랫폼운영TF", true, "플랫폼 운영 총괄", "주담당", true, UserTeamStatus.ACTIVE),
            new UserTeamSeedSpec("ops.head@ibank.com", "솔루션개발사업부", "플랫폼개발팀", false, "운영 협업", "겸임", false, UserTeamStatus.ACTIVE),
            new UserTeamSeedSpec("member@ibank.com", "솔루션사업부", "운영지원팀", false, "운영 지원", "주담당", true, UserTeamStatus.ACTIVE),
            new UserTeamSeedSpec("member@ibank.com", "솔루션사업부", "운영정산TF", false, "정산 지원", "겸임", false, UserTeamStatus.ACTIVE)
    );
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
        Map<String, Long> departmentIdsByName = seedDepartmentShells(DEPARTMENT_SEEDS);
        Map<String, Long> userIdsByEmail = seedUsers(USER_SEEDS, departmentIdsByName);
        synchronizeDepartments(DEPARTMENT_SEEDS, departmentIdsByName, userIdsByEmail);
        Map<String, Long> teamIdsByKey = seedTeams(TEAM_SEEDS);
        seedUserTeams(USER_TEAM_SEEDS, userIdsByEmail, teamIdsByKey);
    }

    /**
     * 사용자 FK 확보 전 단계에서 부서 row 를 미리 확보한다.
     */
    private Map<String, Long> seedDepartmentShells(List<DepartmentSeedSpec> specs) {
        Map<String, Long> departmentIdsByName = new LinkedHashMap<>();
        for (DepartmentSeedSpec spec : specs) {
            departmentIdsByName.put(spec.departmentName(), ensureDepartmentShell(spec));
        }
        return departmentIdsByName;
    }

    /**
     * 사용자를 먼저 만든 뒤 부서장 FK 와 상태를 최종 목표값으로 맞춘다.
     */
    private void synchronizeDepartments(List<DepartmentSeedSpec> specs,
                                        Map<String, Long> departmentIdsByName,
                                        Map<String, Long> userIdsByEmail) {
        for (DepartmentSeedSpec spec : specs) {
            Department department = departmentRepository.findById(departmentIdsByName.get(spec.departmentName()))
                    .orElseThrow();
            Long headUserId = spec.headUserEmail() == null ? null : userIdsByEmail.get(spec.headUserEmail());
            department.synchronizeSeedProfile(
                    spec.departmentName(),
                    spec.description(),
                    headUserId,
                    spec.statusCode()
            );
            log.info("[LocalSeed] 부서 보정 - name={} status={} headUserEmail={}",
                    spec.departmentName(), spec.statusCode(), spec.headUserEmail());
        }
    }

    /**
     * 주어진 이름의 부서가 없으면 새로 저장하고, 있으면 기존 id 를 반환한다.
     * department_name UNIQUE 제약이 있어 같은 스펙을 재기동해도 동일 row 를 재사용한다.
     */
    private Long ensureDepartmentShell(DepartmentSeedSpec spec) {
        return departmentRepository.findByDepartmentName(spec.departmentName())
                .map(Department::getId)
                .orElseGet(() -> {
                    Department saved = departmentRepository.save(Department.create(spec.departmentName(), spec.description()));
                    log.info("[LocalSeed] 부서 생성 - name={} id={}", spec.departmentName(), saved.getId());
                    return saved.getId();
                });
    }

    /**
     * 사용자 시드 목록을 순회하며 부서 소속/권한/재직 상태를 목표값으로 맞춘다.
     */
    private Map<String, Long> seedUsers(List<UserSeedSpec> specs, Map<String, Long> departmentIdsByName) {
        Map<String, Long> userIdsByEmail = new LinkedHashMap<>();
        for (UserSeedSpec spec : specs) {
            userIdsByEmail.put(spec.email(), ensureUser(spec, departmentIdsByName));
        }
        return userIdsByEmail;
    }

    /**
     * 주어진 이메일의 사용자가 없으면 새로 저장한다. 이미 있으면 목표 부서/권한/재직 상태로 보정한다.
     * 비밀번호는 DEV_PASSWORD 를 BCrypt 로 해시해 저장한다.
     */
    private Long ensureUser(UserSeedSpec spec, Map<String, Long> departmentIdsByName) {
        Long departmentId = departmentIdsByName.get(spec.departmentName());
        return userRepository.findByEmail(spec.email())
                .map(user -> {
                    user.synchronizeSeedProfile(
                            departmentId,
                            spec.userName(),
                            spec.role(),
                            spec.employmentStatus(),
                            spec.positionName(),
                            spec.titleName(),
                            DEFAULT_JOIN_DATE
                    );
                    log.info("[LocalSeed] 사용자 보정 - email={} role={} departmentId={} titleName={}",
                            spec.email(), spec.role(), departmentId, spec.titleName());
                    return user.getId();
                })
                .orElseGet(() -> {
                    User user = User.create(
                            departmentId,
                            spec.userName(),
                            spec.email(),
                            passwordEncoder.encode(DEV_PASSWORD),
                            spec.role(),
                            spec.employmentStatus(),
                            spec.positionName(),
                            spec.titleName(),
                            DEFAULT_JOIN_DATE,
                            null,
                            null
                    );
                    User saved = userRepository.save(user);
                    log.info("[LocalSeed] 사용자 생성 - email={} role={} departmentId={} titleName={}",
                            spec.email(), spec.role(), departmentId, spec.titleName());
                    return saved.getId();
                });
    }

    /**
     * 팀 시드 목록을 순회하며 ACTIVE/INACTIVE 상태와 legacy 이름 치환을 함께 보장한다.
     */
    private Map<String, Long> seedTeams(List<TeamSeedSpec> specs) {
        Map<String, Long> teamIdsByKey = new LinkedHashMap<>();
        for (TeamSeedSpec spec : specs) {
            teamIdsByKey.put(teamKey(spec.departmentName(), spec.teamName()), ensureTeam(spec));
        }
        return teamIdsByKey;
    }

    /**
     * 로컬 검증용 팀이 없으면 생성하고, 있으면 이름/상태/설명을 목표값으로 보정한다.
     * legacy 팀명이 남아 있으면 같은 row 를 재사용해 현재 스펙 이름으로 수렴시킨다.
     */
    private Long ensureTeam(TeamSeedSpec spec) {
        return findTeamByNames(spec.teamName(), spec.legacyTeamNames())
                .map(team -> {
                    team.synchronizeSeedProfile(
                            spec.teamName(),
                            spec.statusCode(),
                            spec.description(),
                            DEFAULT_JOIN_DATE,
                            null
                    );
                    log.info("[LocalSeed] 팀 보정 - teamName={} status={}",
                            spec.teamName(), spec.statusCode());
                    return team.getId();
                })
                .orElseGet(() -> {
                    Team saved = teamRepository.save(Team.create(
                            spec.teamName(),
                            spec.statusCode(),
                            spec.description(),
                            DEFAULT_JOIN_DATE,
                            null
                    ));
                    log.info("[LocalSeed] 팀 생성 - teamName={} teamId={} status={}",
                            spec.teamName(), saved.getId(), spec.statusCode());
                    return saved.getId();
                });
    }

    /**
     * 사용자-팀 시드 목록을 순회하며 주소속/리더 여부를 목표값으로 맞춘다.
     */
    private void seedUserTeams(List<UserTeamSeedSpec> specs,
                               Map<String, Long> userIdsByEmail,
                               Map<String, Long> teamIdsByKey) {
        for (UserTeamSeedSpec spec : specs) {
            ensureUserTeam(
                    userIdsByEmail.get(spec.userEmail()),
                    teamIdsByKey.get(teamKey(spec.departmentName(), spec.teamName())),
                    spec.isLeader(),
                    spec.teamRole(),
                    spec.allocation(),
                    spec.isPrimary(),
                    spec.statusCode()
            );
        }
    }

    /**
     * 사용자-팀 관계가 없으면 생성하고, 있으면 역할/주소속 여부를 목표값으로 보정한다.
     * 로컬 로그인 후 현재 사용자 조회 API 가 항상 ACTIVE team 컨텍스트를 돌려줄 수 있도록 상태까지 함께 유지한다.
     */
    private void ensureUserTeam(Long userId,
                                Long teamId,
                                boolean isLeader,
                                String teamRole,
                                String allocation,
                                boolean isPrimary,
                                UserTeamStatus statusCode) {
        userTeamRepository.findByUserIdAndTeamId(userId, teamId)
                .ifPresentOrElse(userTeam -> {
                    userTeam.synchronizeSeedProfile(isLeader, teamRole, allocation, isPrimary, statusCode);
                    log.info("[LocalSeed] 사용자-팀 관계 보정 - userId={} teamId={} leader={} role={} allocation={} primary={} status={}",
                            userId, teamId, isLeader, teamRole, allocation, isPrimary, statusCode);
                }, () -> {
                    userTeamRepository.save(UserTeam.create(userId, teamId, isLeader, teamRole, allocation, isPrimary, statusCode));
                    log.info("[LocalSeed] 사용자-팀 관계 생성 - userId={} teamId={} leader={} role={} allocation={} primary={} status={}",
                            userId, teamId, isLeader, teamRole, allocation, isPrimary, statusCode);
                });
    }

    /**
     * 현재 스펙 이름을 우선하고, 없으면 legacy 이름 순서로 이미 존재하는 팀 row 를 찾는다.
     * soft-delete 된 팀도 복구 후보에 포함해 로컬 시드 재실행이 멱등하게 유지되도록 한다.
     */
    private java.util.Optional<Team> findTeamByNames(String currentName, List<String> legacyNames) {
        java.util.Optional<Team> current = teamRepository.findFirstByTeamNameOrderByDeletedAtDesc(currentName);
        if (current.isPresent()) {
            return current;
        }
        for (String legacyName : legacyNames) {
            java.util.Optional<Team> legacy = teamRepository.findFirstByTeamNameOrderByDeletedAtDesc(legacyName);
            if (legacy.isPresent()) {
                return legacy;
            }
        }
        return java.util.Optional.empty();
    }

    /** 부서명과 팀명을 합쳐 사용자-팀 시드 lookup key 를 만든다. */
    private String teamKey(String departmentName, String teamName) {
        return departmentName + ":" + teamName;
    }

    private record DepartmentSeedSpec(String departmentName,
                                      String description,
                                      DepartmentStatus statusCode,
                                      String headUserEmail) {
    }

    private record UserSeedSpec(String email,
                                String departmentName,
                                UserRole role,
                                String userName,
                                String positionName,
                                String titleName,
                                EmploymentStatus employmentStatus) {
    }

    private record TeamSeedSpec(String departmentName,
                                String teamName,
                                TeamStatus statusCode,
                                String description,
                                List<String> legacyTeamNames) {
    }

    private record UserTeamSeedSpec(String userEmail,
                                    String departmentName,
                                    String teamName,
                                    boolean isLeader,
                                    String teamRole,
                                    String allocation,
                                    boolean isPrimary,
                                    UserTeamStatus statusCode) {
    }

}
