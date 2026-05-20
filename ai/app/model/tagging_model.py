from pydantic import BaseModel, ConfigDict, Field

TAG_DESCRIPTION_MAX_LENGTH = 150


class MetaTag(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    tag_id: int = Field(alias="tagId", ge=1)
    tag_name: str = Field(alias="tagName", min_length=1, max_length=100)
    description: str | None = Field(default=None, max_length=TAG_DESCRIPTION_MAX_LENGTH)
    usage_count: int = Field(default=0, alias="usageCount", ge=0)


class NewTag(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    tag_name: str = Field(alias="tagName", min_length=1, max_length=100)
    description: str = Field(min_length=1, max_length=TAG_DESCRIPTION_MAX_LENGTH)


class TaggingChainResult(BaseModel):
    existing_tags: list[str] = Field(default_factory=list)
    new_tags: list[NewTag] = Field(default_factory=list)


class TaggingResult(BaseModel):
    existing_tags: list[str] = Field(default_factory=list)
    new_tags: list[NewTag] = Field(default_factory=list)

    @property
    def tag_names(self) -> list[str]:
        return [*self.existing_tags, *(tag.tag_name for tag in self.new_tags)]
