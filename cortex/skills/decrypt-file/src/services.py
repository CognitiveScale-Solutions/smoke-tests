from io import BytesIO
from zipfile import ZipFile

from cortex.skill import SkillClient

from models import DecryptRequest
from cortex.content import ManagedContentClient


def decrypt_file(config: DecryptRequest):
    client: ManagedContentClient = ManagedContentClient(url=config.apiEndpoint, token=config.token,
                                                        project=config.projectId)
    skill_client = SkillClient(project=config.projectId, url=config.apiEndpoint, version=6, token=config.token)
    response = client.download(config.filename, config.projectId)
    password = None if config.fileKey is None else config.fileKey.encode("utf-8")
    zip_data = BytesIO(response.data)
    training_valid = False
    with ZipFile(zip_data, 'r') as zipObj:
        for filename in zipObj.namelist():
            if filename == config.trainingFile:
                training_valid = True
            buffered = zipObj.read(filename, pwd=password)

            client.upload(filename, project=config.projectId, stream=buffered, stream_name=filename,
                          content_type="octet/stream")
    if config.trainingFile:
        if training_valid:
            message = {"filename": config.trainingFile}
            output_name = "training"
        else:
            message = {"error": f"{config.filename} missing expected training file: {config.trainingFile}"}
            output_name = "error"
        skill_client.send_message(activation=config.activationId, channel=config.channelId,
                                  output_name=output_name,
                                  message=message)
