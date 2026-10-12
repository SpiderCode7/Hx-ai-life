from fastapi import FastAPI
from pydantic import BaseModel
import threading
from typing import Optional

from dianping.dianping import run_dianping

app = FastAPI()
device_lock = threading.Lock()


class DianpingRequest(BaseModel):
    query: str
    # 本次优化：把目标名称和搜索上下文传给自动化脚本，用于详情页名称核验。
    targetName: Optional[str] = None
    placeType: Optional[str] = None
    nearbyPlace: Optional[str] = None


@app.post("/dianping")
def dianping(request: DianpingRequest):
    with device_lock:
        # 本次优化：保留 query，同时传递名称、类型和附近地点上下文，不再传递地址校验参数。
        return run_dianping(
            request.query,
            target_name=request.targetName,
            place_type=request.placeType,
            nearby_place=request.nearbyPlace,
        )
