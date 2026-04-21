package com.ibank.axwms.domain.devsupport.bootstrap;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
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

        ensureUser("director@ibank.com", devDepartmentId, UserRole.DIRECTOR, "김이사", "이사");
        ensureUser("dept@ibank.com", devDepartmentId, UserRole.DEPT_HEAD, "박부장", "부장");
        ensureUser("member@ibank.com", salesDepartmentId, UserRole.MEMBER, "이사원", "사원");
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
    private void ensureUser(String email, Long departmentId, UserRole role, String userName, String positionName) {
        if (userRepository.existsByEmail(email)) {
            log.info("[LocalSeed] 사용자 skip - email={} (이미 존재)", email);
            return;
        }
        User user = User.create(
                departmentId,
                userName,
                email,
                passwordEncoder.encode(DEV_PASSWORD),
                role,
                EmploymentStatus.ACTIVE,
                positionName,
                null,
                DEFAULT_JOIN_DATE
        );
        userRepository.save(user);
        log.info("[LocalSeed] 사용자 생성 - email={} role={} departmentId={}", email, role, departmentId);
    }
}
