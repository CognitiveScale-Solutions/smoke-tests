import argparse
import json
import sys

from io import BytesIO

from cortex.run import Run
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split, GridSearchCV

from models import TrainModel, CatEncoder
from cortex.content import ManagedContentClient
from cortex.experiment import ExperimentClient, Experiment
import pandas as pd


def train(message: TrainModel):
    print(f"Downloading {message.dataset} from ManagedContent")
    client = ManagedContentClient(project_id=message.project_id, token=message.token, url=message.api_endpoint)
    content = client.download(message.dataset, project=message.project_id)
    df = pd.read_csv(BytesIO(content.data))
    experiment_client = ExperimentClient(message.api_endpoint, version=6, token=message.token)
    experiment: Experiment = Experiment.get_experiment(message.experiment_name, project=message.project_id,
                                                       client=experiment_client)
    run: Run = experiment.start_run()

    run.start()
    cat_columns = [
        'checkingstatus',
        'history',
        'purpose',
        'savings',
        'employ',
        'status',
        'others',
        'property',
        'age',
        'otherplans',
        'housing',
        'job',
        'telephone',
        'foreign'
    ]

    label_column = 'outcome'
    y = df[label_column]
    X = df.drop(label_column, axis=1)

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.20, random_state=42)

    encoder = CatEncoder(cat_columns, X)
    logistic_model, accuracy = build_model((encoder(X_train.values), y_train),
                                           'Logistic classifier',
                                           test=(encoder(X_test.values), y_test))

    run.log_metric("accuracy", accuracy)
    run.stop()
    run.log_artifact("encoder", encoder)
    run.log_artifact("model", logistic_model)


def build_model(data, name, test=None):
    if test is None:
        test = data

    parameters = {'C': (0.5, 1.0, 2.0), 'solver': ['lbfgs'], 'max_iter': [1000]}
    m = LogisticRegression()
    model = GridSearchCV(m, parameters, cv=3)
    model.fit(data[0], data[1])

    # Assess on the test data
    accuracy = model.score(test[0], test[1].values)
    print(f"Model '{name}' accuracy is {accuracy}")
    return model, accuracy

def init_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description='Downloads a zip file decrypts it, and uploads individual files to MC')
    parser.add_argument("-m", help="The Cortex Message received as part of the payload", dest="config",
                        type=TrainModel.parse_raw)
    return parser


def parse_args(args) -> argparse.Namespace:
    parser = init_parser()
    return parser.parse_args(args)


if __name__ == "__main__":
    payload =parse_args(None)

    train(payload.config)
