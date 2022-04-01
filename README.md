# Smoke Tests
## Purpose
Validate and ensure that your Cortex Fabric environment is minimally configured to run and deploy your solution.  It
does this by deploying a single action with four actions to cover/test the minimal features to smoke test.

## Pre-requisites
1. Install the Cortex Tools by following the steps [here](https://cognitivescale.github.io/cortex-fabric/docs/getting-started/install-cortex-tools)
   You will minimally need, the Cortex CLI, and Docker installed in your environment before you can continue
2. Download and install JQ.  It's a json parser for the CLI.  The Makefiles depend on it.  Documentation and steps can
    be found [here](https://stedolan.github.io/jq/download/)
3. Log into the Cortex console, and download your PAT file. Once you have your PAT, you can run the CLI to configure the Cortex CLI
````shell
cortex configure --profile <profile_name>  --project <project_name>
````
4.  Activate the profile by running the command
````shell
cortex configure set-profile <profile_name>
````
**Note**:  It is important that you define in your configuration the project you want to run smoke-tests on.  The tests
will create secrets, upload content, and create custom types.  In most cases, we want this siloed so it doesn't pollute
the developer workspaces.

## Deploy the Tests
The deployment of the agents and skills has been automated.  The validation steps are still in flux and will require
manual verification.  

## Step 1:
Run the command `make display-env`.  The output should look like this:
````shell
 % make display-env
Project: smoke-test
Version: 6.0.10 

````
It should display the project name that you will be deploying too, as well as the current version of Cortex deployed.  The
Cortex Fabric version displayed is pulled directly from the API, so if it looks wrong something has gone wrong with your 
installation.  If everything looks right jump to [Step 2](#step-2)

If you get an error message, or fail to get a valid response value then either the CLI tool is not properly configured
or the cluster installation needs to be reviewed.
### No Values Set
This happens if the file .cortex is generated during an error, and the script fails to clean up...you will see this
````shell
% make display-env
Project: 
Version:  

````
To correct this error run the command `rm .cortex` or `make clean`

### CLI Not Configured Error
```
/usr/local/lib/node_modules/cortex-cli/src/config.js:189
        throw new Error('Please configure the Cortex CLI by running "cortex configure"');
        ^

Error: Please configure the Cortex CLI by running "cortex configure"
    at module.exports.loadProfile (/usr/local/lib/node_modules/cortex-cli/src/config.js:189:15)
    at module.exports.PrintEnvVars.execute (/usr/local/lib/node_modules/cortex-cli/src/commands/configure.js:180:25)
    at /usr/local/lib/node_modules/cortex-cli/bin/cortex-configure.js:84:35
    at Command.wrapped (/usr/local/lib/node_modules/cortex-cli/src/commander.js:28:9)
    at Command.listener [as _actionHandler] (/usr/local/lib/node_modules/cortex-cli/node_modules/commander/index.js:426:31)
    at Command._parseCommand (/usr/local/lib/node_modules/cortex-cli/node_modules/commander/index.js:1002:14)
    at Command._dispatchSubcommand (/usr/local/lib/node_modules/cortex-cli/node_modules/commander/index.js:953:18)
    at Command._parseCommand (/usr/local/lib/node_modules/cortex-cli/node_modules/commander/index.js:970:12)
    at Command.parse [as _parse] (/usr/local/lib/node_modules/cortex-cli/node_modules/commander/index.js:801:10)
    at Command.commander.Command.parse (/usr/local/lib/node_modules/cortex-cli/src/commander.js:47:10)
 
```


## Step 2
Build and Deploy all skills/secrets and content. 
```shell
make all
```
Logging should be fairly verbose.  If there is a problem with any of the configurations, we should fail quickly on the appropriate problem step.

The first step deploys the Secrets using the CLI:  If secrets aren't saving we will fail there (Vault/HashiCorp, etc.).
The second step uploads content to Managed Content:  This is backed my MinIO, validate that the MinIO configurations are correct.
Finally if the actions fail to deploy then that means that the probable root cause is Docker. If the client is using an external
url..then I probably need to update the scripts to do it differently.  Open a JIRA ticket and I'll figure out how to 
modify this so it works for everyone.
At this point, if nothing has failed we have validated all services are up and appear to be running.  In the next step we're going to ensure that
they are working by running the test invocations to check that the internals work

## Step 3
So up to this point we've been fully automated.  The final test is making sure the plumbing works by verifying an agent
The agent will execute the following steps
1. Decrypt-Skill
   1. Download the file defined in cortex/skills/decrypt/skill/test/payload.json file 
   2. Unzip it using the secret defined in the skill.yaml.  
   3. Upload each individual file back into Managed Content
2. Train-Model
   1. Download the csv defined in the configuration
   2. Register an Experiment, then start a run.  During the run it will train a model
   3. Once training is complete, upload the artifacts (model and encoder) to the ExperimentsAPI
3. REST-Predict
   1. Deployed, not yet wired.  Once everything has run.  We can invoke a prediction to validate that the end-end flow worked and that we can see insights

Alright, now that we know what is happening run the command:
```shell
make train
```

