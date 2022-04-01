#  Copyright (c) 2022. Cognitive Scale Inc. All Rights Reserved. Confidential and Proprietary.
from pydantic import BaseModel
from typing import Optional

from sklearn import preprocessing
from sklearn.preprocessing import StandardScaler
import numpy as np

class CreditStatus(BaseModel):
    checkingstatus: str
    duration: str
    history: str
    purpose: str
    amount: str
    savings: str
    employ: str
    installment: str
    status: str
    others: str
    residence: str
    property: str
    age: str
    otherplans: str
    housing: str
    cards: str
    job: str
    liable: str
    telephone: str
    foreign: str


class EvaluateRequest(BaseModel):
    apiEndpoint: str
    token: str
    payload: CreditStatus
    properties: dict
    projectId: str
    channelId: Optional[str]
    activationId: Optional[str]

    @property
    def experiment(self) -> str:
        return self.properties.get("experiment_name", None)

    @property
    def run_id(self) -> str:
        return self.properties.get("run_id", None)


class CatEncoder:
    def __init__(self, cat_columns, data, normalize: bool = True):
        self.cat_indexes = [data.columns.get_loc(name) for name in cat_columns]
        self.num_indexes = [idx for idx in range(len(data.columns)) if idx not in self.cat_indexes]
        self.encoder = preprocessing.OneHotEncoder()
        self.encoder.fit(data[cat_columns])
        self.num_columns = list(data.columns[self.num_indexes])
        self.cat_columns = cat_columns
        cat_transformed_names = self.encoder.get_feature_names(input_features=self.cat_columns)
        self._transformed_column_names = self.num_columns + list(cat_transformed_names)
        if normalize:
            self.normalizer = StandardScaler()
            self.normalizer.fit(data.iloc[:, self.num_indexes])
        else:
            self.normalizer = None

    def __call__(self, x):
        numeric = x[:, self.num_indexes]
        if self.normalizer is not None:
            numeric = self.normalizer.transform(numeric)
        categorical = self.encoder.transform(x[:, self.cat_indexes]).toarray()
        return np.concatenate((numeric, categorical), axis=1)

    @property
    def transformed_features(self):
        return self._transformed_column_names
