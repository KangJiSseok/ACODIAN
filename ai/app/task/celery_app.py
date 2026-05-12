from celery import Celery

from app.config.settings import settings

celery_app = Celery(
    "ax_wms_ai",
    broker=settings.celery_broker_url or settings.redis_url,
    backend=settings.celery_result_backend or settings.redis_url,
    include=[
        "app.task.worklog_tasks",
        "app.task.tagging_tasks",
        "app.task.embedding_tasks",
    ],
)

celery_app.conf.update(
    task_serializer="json",
    result_serializer="json",
    accept_content=["json"],
    timezone="Asia/Seoul",
    task_acks_late=True,
    task_reject_on_worker_lost=True,
    task_always_eager=settings.celery_task_always_eager,
)
