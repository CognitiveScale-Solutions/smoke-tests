import json
import argparse
from models import DecryptRequest
from services import decrypt_file


# Strong Note - this has not been optimized for scale...only for a very tiny smoke test.  You will need to be/do better
# if you want to operationalize this....it will run out of memory since I'm not streaming the data down

def init_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description='Downloads a zip file decrypts it, and uploads individual files to MC')
    parser.add_argument("-m", help="The Cortex Message received as part of the payload", dest="config",
                        type=DecryptRequest.parse_raw)
    return parser


def parse_args(args) -> argparse.Namespace:
    parser = init_parser()
    return parser.parse_args(args)


if __name__ == '__main__':
    values: argparse.Namespace = parse_args(None)
    config = values.config
    decrypt_file(config)
