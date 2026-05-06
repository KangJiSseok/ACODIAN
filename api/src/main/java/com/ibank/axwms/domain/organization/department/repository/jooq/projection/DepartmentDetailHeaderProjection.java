package com.ibank.axwms.domain.organization.department.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_DEPARTMENT;

import com.ibank.axwms.global.jooq.tables.TbUser;
import org.jooq.Record;

/** 부서 상세 header 는 팀 집계와 독립적으로 조회되어 팀이 없는 부서도 부서장 정보를 보존한다. */
public record DepartmentDetailHeaderProjection(
        Long departmentId,
        String departmentName,
        Long departmentHeadUserId,
        String departmentHeadUserName
) {

    /** 부서장 alias 를 함께 받아 header query 의 left join 결과를 projection vocabulary 로 변환한다. */
    public static DepartmentDetailHeaderProjection from(Record record, TbUser headUser) {
        return new DepartmentDetailHeaderProjection(
                record.get(TB_DEPARTMENT.DEPARTMENT_ID),
                record.get(TB_DEPARTMENT.DEPARTMENT_NAME),
                record.get(TB_DEPARTMENT.DEPARTMENT_HEAD_USER_ID),
                record.get(headUser.USER_NAME)
        );
    }
}
