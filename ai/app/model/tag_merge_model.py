from pydantic import BaseModel, ConfigDict, Field

from app.model.tagging_model import TAG_DESCRIPTION_MAX_LENGTH


class GenerateTagMergeCandidatesRequest(BaseModel):
    max_candidate_count: int = Field(default=5, alias="maxCandidateCount", ge=1, le=5)
    max_group_count: int = Field(default=20, alias="maxGroupCount", ge=1, le=100)
    min_usage_count: int = Field(default=0, alias="minUsageCount", ge=0)
    tag_limit: int = Field(default=200, alias="tagLimit", ge=10, le=1000)


class TagMergeCandidateTag(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    tag_id: int = Field(alias="tagId", ge=1)
    tag_name: str = Field(alias="tagName", min_length=1, max_length=100)
    usage_count: int = Field(default=0, alias="usageCount", ge=0)


class TagMergeCandidateGroup(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    merge_target_tag: TagMergeCandidateTag = Field(alias="mergeTargetTag")
    result_description: str | None = Field(
        default=None,
        alias="resultDescription",
        max_length=TAG_DESCRIPTION_MAX_LENGTH,
    )
    merge_candidate_tags: list[TagMergeCandidateTag] = Field(alias="mergeCandidateTags")


class GenerateTagMergeCandidatesResponse(BaseModel):
    items: list[TagMergeCandidateGroup] = Field(default_factory=list)


class TagMergeChainGroup(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    target_tag_id: int = Field(alias="targetTagId", ge=1)
    result_description: str | None = Field(
        default=None,
        alias="resultDescription",
        max_length=TAG_DESCRIPTION_MAX_LENGTH,
    )
    candidate_tag_ids: list[int] = Field(alias="candidateTagIds", default_factory=list)


class TagMergeChainResult(BaseModel):
    groups: list[TagMergeChainGroup] = Field(default_factory=list)
