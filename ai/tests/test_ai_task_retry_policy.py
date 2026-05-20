from app.task.retry_policy import is_retryable_ai_error


class FakeApiError(Exception):
    def __init__(self, status_code: int) -> None:
        super().__init__(f"status={status_code}")
        self.status_code = status_code


def test_retryable_ai_error_accepts_temporary_provider_failure() -> None:
    assert is_retryable_ai_error(FakeApiError(503)) is True


def test_retryable_ai_error_accepts_rate_limit() -> None:
    assert is_retryable_ai_error(FakeApiError(429)) is True


def test_retryable_ai_error_rejects_bad_request() -> None:
    assert is_retryable_ai_error(FakeApiError(400)) is False


def test_retryable_ai_error_accepts_timeout() -> None:
    assert is_retryable_ai_error(TimeoutError("timeout")) is True
