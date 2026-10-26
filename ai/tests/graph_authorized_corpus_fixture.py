from types import SimpleNamespace


class _FixtureResult:
    def __init__(self, rows: list[SimpleNamespace]) -> None:
        self._rows = rows

    def all(self) -> list[SimpleNamespace]:
        return self._rows


class FilteringAuthorizedCorpusSession:
    def __init__(self) -> None:
        self.worklogs = {
            101: self._worklog(101, 7, "allowed", False),
            202: self._worklog(202, 8, "team 8 leak", False),
            303: self._worklog(303, 7, "deleted worklog", True),
            404: self._worklog(404, 9, "deleted team", False),
            88: self._worklog(88, 7, "same team predecessor", False),
            99: self._worklog(99, 8, "cross team predecessor", False),
            77: self._worklog(77, 7, "deleted predecessor", True),
            66: self._worklog(66, 9, "deleted team predecessor", False),
        }
        self.deleted_team_ids = {9}
        self.dependencies = [(101, 88), (101, 99), (101, 77), (101, 66)]

    async def execute(self, statement: object) -> _FixtureResult:
        compiled = statement.compile()
        sql = str(compiled)
        params = compiled.params
        allowed_team_ids = set(params.get("team_id_1") or [])
        worklog_ids = set(params.get("worklog_id_1") or [])

        if "row_number()" in sql:
            return _FixtureResult(self._candidate_rows(allowed_team_ids))
        if "tb_worklog_dependency" in sql:
            return _FixtureResult(self._predecessor_rows(worklog_ids, allowed_team_ids))
        if "tb_worklog_tag" in sql:
            return _FixtureResult([])
        return _FixtureResult(
            [
                worklog
                for worklog in self.worklogs.values()
                if worklog.worklog_id in worklog_ids
                and self._is_visible(worklog, allowed_team_ids)
            ]
        )

    def _candidate_rows(self, allowed_team_ids: set[int]) -> list[SimpleNamespace]:
        return [
            SimpleNamespace(
                worklog_id=worklog.worklog_id,
                team_id=worklog.team_id,
                distance=0.1 + (worklog.worklog_id / 1000),
                chunk_index=0,
                chunk_content=worklog.title,
            )
            for worklog in self.worklogs.values()
            if worklog.worklog_id in {101, 202, 303, 404}
            and self._is_visible(worklog, allowed_team_ids)
        ]

    def _predecessor_rows(
        self,
        worklog_ids: set[int],
        allowed_team_ids: set[int],
    ) -> list[SimpleNamespace]:
        rows: list[SimpleNamespace] = []
        for worklog_id, predecessor_id in self.dependencies:
            predecessor = self.worklogs[predecessor_id]
            if worklog_id in worklog_ids and self._is_visible(predecessor, allowed_team_ids):
                rows.append(
                    SimpleNamespace(
                        worklog_id=worklog_id,
                        depends_on_worklog_id=predecessor_id,
                        team_id=predecessor.team_id,
                        title=predecessor.title,
                    )
                )
        return rows

    def _is_visible(self, worklog: SimpleNamespace, allowed_team_ids: set[int]) -> bool:
        return (
            not worklog.is_deleted
            and worklog.team_id not in self.deleted_team_ids
            and (not allowed_team_ids or worklog.team_id in allowed_team_ids)
        )

    @staticmethod
    def _worklog(
        worklog_id: int,
        team_id: int,
        title: str,
        is_deleted: bool,
    ) -> SimpleNamespace:
        return SimpleNamespace(
            worklog_id=worklog_id,
            title=title,
            request_content=None,
            work_content=title,
            author_id=5,
            user_name="김도윤",
            team_id=team_id,
            team_name=f"team-{team_id}",
            is_deleted=is_deleted,
        )
