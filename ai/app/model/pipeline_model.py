from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class WorklogPipelineRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    worklog_id: int = Field(alias="worklogId", ge=1)
    request_content: str | None = Field(default=None, alias="requestContent")
    work_content: str = Field(alias="workContent", min_length=1)
    author_id: int = Field(alias="authorId", ge=1)
    team_id: int = Field(alias="teamId", ge=1)
    department_id: int | None = Field(default=None, alias="departmentId", ge=1)


class PipelineAcceptedResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    worklog_id: int = Field(alias="worklogId")
    summary_task_id: str = Field(alias="summaryTaskId")
    tagging_task_id: str = Field(alias="taggingTaskId")
    status: Literal["ACCEPTED"] = "ACCEPTED"
