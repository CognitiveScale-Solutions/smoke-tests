#  Copyright (c) 2022. Cognitive Scale Inc. All Rights Reserved. Confidential and Proprietary.
import os

import pandas as pd
import uvicorn
from cortex.experiment import ExperimentClient, Experiment
from fastapi import FastAPI, Depends
from models import CreditStatus, EvaluateRequest
from sklearn.model_selection import train_test_split

app = FastAPI()
model = None
encoder = None
runId = None


def get_model(message: EvaluateRequest):
    global model
    global encoder
    experiment_client = ExperimentClient(message.apiEndpoint, version=6, token=message.token)
    experiment: Experiment = Experiment.get_experiment(message.experiment, project=message.projectId,
                                                       client=experiment_client)
    reload = False
    if model is None:
        reload = True
    if message.run_id:
        model_run = experiment.get_run(run_id=message.run_id)
    elif not reload:
        return message
    else:
        model_run = experiment.last_run()
    if runId != model_run.id:
        model = model_run.get_artifact("model")
        encoder = model_run.get_artifact("encoder")

    return message


@app.post('/invoke')
def run(requestBody: EvaluateRequest = Depends(get_model)):
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
def version():
    return {"payload": {"version": os.getenv("VERSION", "0.0.1"), "model": {
        "runId": runId
    }}}


@app.post("/refresh")
def refresh(requestBody: dict = Depends(get_model)):
    return version()


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=5000)
