from pydantic import BaseModel, Field


class PipelineTriggerRequest(BaseModel):
    worklog_id: int = Field(..., ge=1)
    request_content: str = Field(..., min_length=1)
    work_content: str = Field(..., min_length=1)


class PipelineTriggerResponse(BaseModel):
    worklog_id: int
    status: str
    message: str
