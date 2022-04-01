# Each skill is listed under the subdirectory skills/*.  To deploy a single skill run the command make <skill_name>
#
# Set the version equal to the current cortex deployed version.  If we upgrade we should see all containers and jobs
# Updated to use the same tag
IMAGE_TAG=$(shell curl -s ${CORTEX_URL}/fabric/v4/info | jq .version -r) #Base health check...is Cortex ready
export
SUB_DIRS:=$(notdir $(wildcard cortex/skills/*))
CONTENT:=$(filter-out README.md, $(notdir $(wildcard cortex/content/*)) )
all: init $(CONTENT) $(SUB_DIRS) agent

#Failure here means the vault is probably not configured correctly...or that you haven't preconfigured your PAT file
init: display-env
	@echo "Saving Cortex Secrets"
	@cortex secrets save german-credit.zip german-credit

#Failure here indicates problem with managed content
$(CONTENT): display-env
	@echo "Publishing content to Managed Content"
	cortex content upload $@ cortex/content/$@

#Failure here probably means a problem with docker registry
$(SUB_DIRS):
	@echo "Building, Pushing, and Deploying $@ ${result}"
	@echo "Project: ${CORTEX_PROJECT}"
	@echo "Version: ${IMAGE_TAG}"
	@$(MAKE) -C skills/$@ all

#IDK why an agent would fail...but this is the last step before we can validate running containers
agent:
	@echo "Deploying agent"
	cortex agents save -y agent.yaml

.cortex:Makefile
	$(shell cortex configure env > ".cortex")


display-env:
	@echo "Project: ${CORTEX_PROJECT}"
	@echo "Version: ${IMAGE_TAG}"

#Validate that you're picking up and deploying everything you want need
list:
	@echo "Cortex Content:"
		@$(foreach dir,$(CONTENT),echo "*  $(dir)";)
	@echo "Cortex Skills: "
	 @$(foreach dir,$(SUB_DIRS),echo "*  $(dir)";)

-include .cortex