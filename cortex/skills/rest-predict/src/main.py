#  Copyright (c) 2022. Cognitive Scale Inc. All Rights Reserved. Confidential and Proprietary.
import os

import pandas as pd
import uvicorn
from cortex.experiment import ExperimentClient, Experiment
from fastapi import FastAPI, Depends
from models import CreditStatus, EvaluateRequest, BaseRequest, Version, ModelInfo

app = FastAPI()
model = None
encoder = None
runId = None
modelInfo = ModelInfo()
version = Version(version=os.getenv("VERSION", "1.2.3"), model=modelInfo)


def need_model(message: EvaluateRequest):
    # Meh don't feel like learning how python downgrades so this is the helper to cast and recast
    get_model(message, True)
    return message


def get_model(message: BaseRequest, reload=False):
    global model
    global encoder
    global modelInfo
    experiment_client = ExperimentClient(message.apiEndpoint, version=6, token=message.token)
    experiment: Experiment = Experiment.get_experiment(message.experiment, project=message.projectId,
                                                       client=experiment_client)
    reload = reload or modelInfo.requires_reload(message.experiment, message.run_id)
    if model is None:
        reload = True
    if message.run_id:
        model_run = experiment.get_run(run_id=message.run_id)
    elif not reload:
        return message
    else:
        model_run = experiment.last_run()
    if version.model.run_id != model_run.id:
        model = model_run.get_artifact("model")
        encoder = model_run.get_artifact("encoder")
    modelInfo.run_id = model_run.id
    modelInfo.experiment_name = message.experiment
    return message


@app.post('/invoke')
def run(requestBody: EvaluateRequest = Depends(need_model)):
    result = [None, "Loan Approved", "Loan Declined"]
    global encoder
    global model
    global runId
    data = pd.DataFrame.from_records([requestBody.payload.dict()]).to_numpy()

    if encoder:
        data = encoder(data)
    outcome = model.predict(data)

    return {"payload": {"outcome": result[outcome[0]]}}


@app.get("/version")
@app.post("/version")
def versionStatus():
    return {"payload": version.dict()}


@app.post("/refresh")
def refresh(requestBody: BaseRequest = Depends(get_model)):
    return {"payload": version.dict()}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=5000)
