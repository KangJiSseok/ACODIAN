import asyncio
import logging

from app.client.tagging_client import TaggingClient
from app.model.tagging_model import MetaTag
from app.service.tagging_service import TaggingService
from app.task.celery_app import celery_app

logger = logging.getLogger(__name__)


@celery_app.task(name="worklog.tagging")
def generate_worklog_tags(
    worklog_id: int,
    work_content: str,
) -> dict[str, int | list[str]]:
    return asyncio.run(
        _generate_worklog_tags(
            worklog_id=worklog_id,
            work_content=work_content,
        )
    )


async def _generate_worklog_tags(
    worklog_id: int,
    work_content: str,
) -> dict[str, int | list[str]]:
    async with TaggingClient() as tagging_client:
        existing_tags = await tagging_client.list_tags()
        tagging_result = await TaggingService().generate_tags(
            work_content=work_content,
            existing_tags=existing_tags,
        )

        existing_tag_ids = _find_existing_tag_ids(
            tag_names=tagging_result.existing_tags,
            existing_tags=existing_tags,
        )
        await tagging_client.apply_worklog_ai_tags(
            worklog_id=worklog_id,
            existing_tag_ids=existing_tag_ids,
            new_tag_names=tagging_result.new_tags,
        )
    logger.info(
        "태그 생성 완료: worklog_id=%s, existing_tags=%s, new_tags=%s",
        worklog_id,
        tagging_result.existing_tags,
        tagging_result.new_tags,
    )

    return {
        "worklog_id": worklog_id,
        "existing_tags": tagging_result.existing_tags,
        "new_tags": tagging_result.new_tags,
    }


def _find_existing_tag_ids(
    tag_names: list[str],
    existing_tags: list[MetaTag],
) -> list[int]:
    tag_id_by_name = {tag.tag_name: tag.tag_id for tag in existing_tags}
    return [
        tag_id_by_name[tag_name]
        for tag_name in tag_names
        if tag_name in tag_id_by_name
    ]
