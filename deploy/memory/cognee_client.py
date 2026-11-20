"""
cognee_client.py â€” Cognee Cloud REST API client.

Environment variables required:
  COGNEE_API_KEY   â€“ API key for authentication
  COGNEE_BASE_URL  â€“ e.g. https://tenant-xxxx.aws.cognee.ai (no trailing slash, no /api)

Endpoint mapping (verified live against the tenant with real curl tests):
  remember_ticket_message  â†’ POST /api/v1/remember  (multipart, datasetName)  [confirmed]
  recall_for_customer      â†’ POST /api/v1/recall     (JSON body, datasetName)  [confirmed]
  resolve_and_improve      â†’ POST /api/v1/remember  (multipart, datasetName)  [confirmed]
  forget_dataset            â†’ POST /api/v1/forget     (JSON body, dataset)     [confirmed]

Design note: both remember_ticket_message and resolve_and_improve now write
into the same per-customer dataset (customer_{id}), rather than the earlier
two-tier session/dataset split. This trades a faster "session cache" write
for a single, fully-tested code path â€” every write costs ~8-10s (real
Cognee ingest against the LLM), acceptable for demo message volumes.
"""

import os
import time
from typing import Any

import httpx

BASE_URL = os.environ.get(
    "COGNEE_BASE_URL",
    "https://tenant-c7dd5c98-efbb-4717-b795-b3b23b68f96d.aws.cognee.ai",
)

# A quota-exhausted provider should not be called on every ticket view.
_quota_cooldown_until = 0.0
_QUOTA_COOLDOWN_SECONDS = 60 * 60


def _provider_available() -> bool:
    return time.monotonic() >= _quota_cooldown_until


def _failure_status(exc: Exception) -> str:
    global _quota_cooldown_until
    if isinstance(exc, httpx.HTTPStatusError) and exc.response.status_code == 429:
        _quota_cooldown_until = time.monotonic() + _QUOTA_COOLDOWN_SECONDS
        return "rate_limited"
    return "unavailable"


def _headers() -> dict:
    api_key = os.environ.get("COGNEE_API_KEY", "").strip()
    if not api_key:
        raise EnvironmentError(
            "COGNEE_API_KEY environment variable is not set. "
            "Set it before calling any cognee_client function."
        )
    return {"X-Api-Key": api_key}


def dataset_for_customer(customer_id: str) -> str:
    prefix = os.environ.get("COGNEE_DATASET_PREFIX", "").strip()
    if not prefix:
        raise EnvironmentError("COGNEE_DATASET_PREFIX is required for this deployment")
    return f"{prefix}{customer_id}"


async def _remember_to_dataset(content: str, dataset: str, filename: str):
    async with httpx.AsyncClient(base_url=BASE_URL, timeout=120.0) as client:
        response = await client.post(
            "/api/v1/remember",
            headers=_headers(),
            data={"datasetName": dataset},
            files={"data": (filename, content.encode(), "text/plain")},
        )
        response.raise_for_status()
        return response.json()


async def remember_ticket_message(content: str, customer_id: str):
    if not _provider_available():
        return "rate_limited"
    dataset = dataset_for_customer(customer_id)
    try:
        await _remember_to_dataset(content, dataset, "ticket_message.txt")
        return "remembered"
    except Exception as exc:
        return _failure_status(exc)


async def recall_for_customer(query: str, customer_id: str):
    if not _provider_available():
        return "rate_limited", []
    dataset = dataset_for_customer(customer_id)
    payload = {
        "query": query,
        "datasetName": dataset,
        "top_k": 5,
    }
    try:
        # Cognee Cloud's real graph completion queries can take close to 60s,
        # so the previous 60.0 timeout was cutting them off right at the edge.
        async with httpx.AsyncClient(base_url=BASE_URL, timeout=120.0) as client:
            response = await client.post(
                "/api/v1/recall",
                headers={**_headers(), "Content-Type": "application/json"},
                json=payload,
            )
            response.raise_for_status()
        results = response.json()
        return "recalled", normalize_results(results, dataset)
    except Exception as exc:
        return _failure_status(exc), []


async def resolve_and_improve(resolution_content: str, customer_id: str):
    if not _provider_available():
        return "rate_limited"
    dataset = dataset_for_customer(customer_id)
    try:
        await _remember_to_dataset(resolution_content, dataset, "resolution.txt")
        return "improved"
    except Exception as exc:
        return _failure_status(exc)


async def forget_dataset(customer_id: str):
    dataset = dataset_for_customer(customer_id)
    payload = {"dataset": dataset}
    try:
        async with httpx.AsyncClient(base_url=BASE_URL, timeout=60.0) as client:
            response = await client.post(
                "/api/v1/forget",
                headers={**_headers(), "Content-Type": "application/json"},
                json=payload,
            )
            response.raise_for_status()
        return "forgotten"
    except Exception as exc:
        return _failure_status(exc)


def normalize_results(result: Any, dataset: str = None) -> list:
    """Fail closed: never expose a memory unless its dataset is explicitly verified.

    Some provider responses include unrelated/default results. Previously a
    no-match fallback returned all records, leaking other customer histories.
    Unknown or unlabelled formats are withheld until their isolation is proven.
    """
    if not dataset:
        return []
    if isinstance(result, dict):
        for key in ("results", "data", "memories"):
            if isinstance(result.get(key), list):
                return normalize_results(result[key], dataset)
        result = [result]
    if not isinstance(result, list):
        return []
    verified = []
    for item in result:
        if not isinstance(item, dict) or item.get("dataset_name") != dataset:
            continue
        content = item.get("text") or item.get("answer")
        if isinstance(content, str) and content.strip():
            verified.append(content[:2000])
    return verified[:5]
