package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import com.ibank.axwms.global.jooq.tables.TbWorklog;
import org.jooq.Record;

/**
 * 선행 업무 대기 위젯의 한 행. (blocked worklog → predecessor) 쌍을 평면적으로 담는다.
 * service 가 myWorklogId 로 그룹핑해 응답으로 가공한다.
 */
public record BlockedPredecessorRowProjection(
        Long myWorklogId,
        String myTitle,
        Long predecessorWorklogId,
        String predecessorTitle,
        String predecessorStatusCode
) {

    /**
     * 같은 tb_worklog 를 my/pred 두 alias 로 self-join 하므로, alias 인스턴스를 추가 인자로 받아
     * 같은 alias 로 select 한 컬럼을 record 에서 꺼낸다 (alias 가 다르면 record.get 이 매칭되지 않음).
     */
    public static BlockedPredecessorRowProjection from(Record record, TbWorklog my, TbWorklog pred) {
        return new BlockedPredecessorRowProjection(
                record.get(my.WORKLOG_ID),
                record.get(my.TITLE),
                record.get(pred.WORKLOG_ID),
                record.get(pred.TITLE),
                record.get(pred.STATUS_CODE)
        );
    }
}
