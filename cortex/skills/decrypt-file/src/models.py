from pydantic import BaseModel


class DecryptRequest(BaseModel):
    apiEndpoint: str
    token: str
    payload: dict
    properties: dict
    projectId: str
    channelId: str
    activationId: str

    @property
    def filename(self) -> str:
        return self.payload.get("filename", None)

    @property
    def fileKey(self) -> str:
        return self.properties.get("encrypted-key", None)

    @property
    def trainingFile(self) -> str:
        return self.properties.get("training-file", None)
