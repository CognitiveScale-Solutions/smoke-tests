### Cortex JDBC and CDATA type Connection example Cortex Skill

This example Skill is a template for JDBC and CDATA connection implemented as Cortex Daemon type Skill. 

#### Prerequisites:
* Docker daemon running  
* Cortex CLI configured to target Cortex DCI  
* Jar build of https://github.com/CognitiveScale/cortex-cdata-plugin in lib directory (included)
* Add JDBC/CDATA driver jar to classpath by any of the below methods
  > Add to build.gradle
  
  > Copy to project's lib directory
  
  > Upload to Cortex managed content in `shared` project and add `managed_content_key` custom parameter in cortex connection 
  > (discouraged as this required instrumentation to add jar to classpath at runtime)
  > To use this run application wit javaagent

* Cortex Connection created in the Cortex DCI (in the project where we will deploy the Skill)
* **For CDATA Connections:**
    * OEM Key and product checksum configured in `shared` project Secret (`cdata-oem-key` and `cdata-prdct-checksum`Secrets)
    * Plugin properties JSON prepared as per CDATA documentation and saved as Secret in the project
      > Follow (CDATA JDBC doc)[https://cdn.cdata.com/help/OWG/jdbc/pg_JDBCconnectcode.htm] of respective database to get driver class name, JDBC URL and prepare plugin properties
      >
      > List of supported databases https://cdn.cdata.com/help/
      > 
      > Snowflake example `{"User":"user1","Password":"****","Url":"https://<tenant1>.snowflakecomputing.com", "Warehouse":"WH1"}`
> This is based on https://sparkjava.com to serve HTTP server. `Main.Java` setts up the route and runs the server.

> `Example` is the interface to plug different JDBC based frameworks. For example Hikari connection pool (HikariExample) and Hibernate (HibernateExample)
> Default is HikariExample, edit Main.java to use HibernateExample. HibernateExample comes with example entity class `Employee` mapped, update this class for desired entities and Hibernate configuration. 
 ```
 Main class 
 * runs web server
 * parse Cortex request
 * uses `cortex-cdata-plugin` to parse Cortex Connection
 * `Example#setup` initializes the connection 
 * `Example#execute` executes SQL query and parse result to Cortex Skill response
```
#### Steps to build and deploy

Set environment variables `DOCKER_PREGISTRY_URL` (like `<docker-registry-url>/<namespace-org>`) and `PROJECT_NAME` (Cortex Project Name), and use build scripts to build and deploy.

Configure Docker auth to the private registry:
1. For Cortex DCI with Docker registry installed use `cortex docker login`
2. For external Docker registries like Google Cloud's GCR etc use their respective CLI for Docker login


   Skills that are deployed may be invoked (run) either independently or within an agent. Update `test/payload.json` with `query` as per database name and `connection_name`, and run `make tests` to invoke the deployed Skill.

For more details about how to build skills go to [Cortex Fabric Documentation - Development - Develop Skills](https://cognitivescale.github.io/cortex-fabric/docs/development/define-skills)