-include .cortex
SHELL:=/bin/bash -O expand_aliases
IMAGE_TAG?=$(or $(shell git rev-parse --short HEAD), latest)
SKILL_NAME=$(notdir $(CURDIR))
#Ugh the URL is prefaced with the scheme (https:) we need to drop it
DOCKER_PREGISTRY_URL=$(lastword $(subst ://, ,$(shell curl -s ${CORTEX_URL}/fabric/v4/info | jq .endpoints.registry.url -r)) )
DOCKER_IMAGE_URL=${DOCKER_PREGISTRY_URL}/${SKILL_NAME}:${IMAGE_TAG}

all: build push deploy
build:
	$(shell cat action.json| jq '.docker = "${DOCKER_IMAGE_URL}" | .environmentVariables = "\"VERSION\"=\"${IMAGE_TAG}\""' > tmp.json && mv tmp.json action.json)
	docker build --build-arg BUILD_VERSION=${IMAGE_TAG} -t ${SKILL_NAME}:latest -t ${DOCKER_IMAGE_URL} .

push:
	@cortex docker login
	docker push ${DOCKER_IMAGE_URL}

deploy: check-env
	#cortex actions deploy action.json --project ${CORTEX_PROJECT} --docker ${DOCKER_IMAGE_URL} --podspec podspec.json
	cortex actions deploy action.json --project ${CORTEX_PROJECT} --docker ${DOCKER_IMAGE_URL}
	cortex types save -y types.yaml
	cortex skills save -y skill.yaml --project ${CORTEX_PROJECT}
	sleep 10s # wait for action containers to pull and deploy

tests: check-env
	cortex skills invoke --params-file ./test/payload.json ${SKILL_NAME} params --project ${CORTEX_PROJECT}

get: check-env
	cortex skills describe ${SKILL_NAME} --project ${CORTEX_PROJECT}

check-env:
	@echo "Version: ${IMAGE_TAG}"
	@echo "Projects: ${CORTEX_PROJECT}"
	@echo "Api Endpoint: ${CORTEX_URL}"
	@echo "Docker registry URL: ${DOCKER_PREGISTRY_URL}"
	@echo "Skill Name: ${SKILL_NAME}"

.cortex:clean
	$(shell cortex configure env > ".cortex")

sample:
	@cat test/payload.json| jq -c '.apiEndpoint="${CORTEX_URL}"| .token="${CORTEX_TOKEN}"|.projectId="${CORTEX_PROJECT}"'


clean:
	@rm -rf .cortex