import CICDEnvUtils
import com.apigee.boot.ConfigType
import com.apigee.boot.Pipeline
import com.apigee.cicd.service.AssetService
import com.apigee.cicd.service.CIEnvInfoService
import com.apigee.cicd.service.DefaultConfigService
import com.apigee.cicd.service.FunctionalTestService
import com.apigee.cicd.service.OrgInfoService
import com.apigee.loader.BootStrapConfigLoad
import com.apigee.cicd.service.DeploymentInfoService
import groovy.json.JsonSlurper
import hudson.AbortException
import hudson.model.Cause
import pom
import Maven
import JenkinsUserUtils
import shell

/*
This pipeline is used to perform CD on apiproxies
 */
def call(String operation, String repoProjectName) {
    node {
        deleteDir()
        def shell = new shell()

        try {

            stage('init') {
                if (!params.projectName) {
                    error "Project Name is Required "
                }
                if (!params.teamName) {
                    error "Team is Required "
                }
                if (!params.artifactId) {
                    error "API Name is Required "
                }
            }
            withCredentials([
                    [$class          : 'UsernamePasswordMultiBinding',
                     credentialsId   : "github-jenkins-configuration",
                     usernameVariable: 'scmUser',
                     passwordVariable: 'scmPassword'],
                    [$class          : 'UsernamePasswordMultiBinding',
                     credentialsId   : "Github-Oauth-access-new",
                     usernameVariable: 'scmClient',
                     passwordVariable: 'scmSecret'],
            ])
                    {

                        withFolderProperties {

                            BootStrapConfigLoad configLoad = new BootStrapConfigLoad();
                            try {
                                scmAPILocation = env.API_SCM_LOCATION
                                scmOauthServerLocation = env.API_SCM_OAUTH_SERVER
                                configLoad.setupConfig("${env.API_SERVER_LOCATION}")
                                configLoad.setupAssetConfiguration("${env.API_SERVER_LOCATION}", "${params.projectName}-${teamName}-${artifactId}")
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                        }
                        stage('Checkout') {
                            shell.pipe("git clone https://${scmUser}:${scmPassword}@github.com/praveenkumars0717/${projectName}-${teamName}-${artifactId}.git")
                        }
                        stage('release-create') {
                            when { 
				expression { 
					params.operation == 'release-create' 
					} 
				}
				steps {
				configFileProvider([configFile(fileId: 'cicd_jfrog_final', variable: 'MAVEN_SETTINGS_XML')]) {
				script {                    
				sh '''
				mvn -s $MAVEN_SETTINGS_XML  jgitflow:release-start -X
				'''
					}
				}
                            }
                        }
						
                    }
            dir("${projectName}-${teamName}-${artifactId}") {
            /*    shell.pipe("git checkout tags/${params.version} -b ${params.version}")
                Maven maven = new Maven()
                JenkinsUserUtils jenkinsUserUtils = new JenkinsUserUtils()
                Npm npm = new Npm()
                def pom = new pom(),
                    proxyRootDirectory = "edge",
                    artifactId = pom.artifactId("./${proxyRootDirectory}/pom.xml"),
                    version = pom.version("./${proxyRootDirectory}/pom.xml"),
                    entityDeploymentInfos   */
            } 
        } 
        catch (any) {
            println any.toString()
            JenkinsUserUtils jenkinsUserUtils = new JenkinsUserUtils()
            currentBuild.result = 'FAILURE'
            DeploymentInfoService.instance.saveDeploymentStatus("FAILURE", env.BUILD_URL, jenkinsUserUtils.getUsernameForBuild())
        }
    }
}
