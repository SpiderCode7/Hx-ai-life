from fastapi import FastAPI
from pydantic import BaseModel
import threading

from dianping.dianping import run_dianping

app = FastAPI()
device_lock = threading.Lock()


class DianpingRequest(BaseModel):
    query: str


@app.post("/dianping")
def dianping(request: DianpingRequest):
    with device_lock:
        return run_dianping(request.query)
