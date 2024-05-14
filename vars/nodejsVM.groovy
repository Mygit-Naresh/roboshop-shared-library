def call(Map configMap) {
    pipeline {
        agent {
            node {
                label 'jenkins-agent-2'
            }
        }
        environment {
            versioncheck = ''
            nexus_url = '172.31.16.69:8081'
        }
        parameters {
            booleanParam(defaultValue: false, description: 'click on checkbox for build', name: 'deploy')
        }
        options {
            timeout(time: 1, unit: 'HOURS')
            ansiColor('xtrem')
        }
        stages {
            stage('get version') {
                steps {
                    script {
                        def jsonfile = readJSON file: 'package.json'
                        versioncheck = jsonfile.version
                        echo "current version is ${versioncheck}"
                    }
                }
            }
            stage('build the code using npm') {
                steps {
                    sh """
                        npm install
                    """
                }
            }
            stage('unit testing') {
                steps {
                    sh """
                        echo "unit testing completed"
                    """
                }
            }
            stage('sonar scanning') {
                steps {
                    sh "sonar-scanner"
                }
            }
            stage("zip files and folders from '${configMap.component}'") {
                steps {
                    sh """
                        ls -la
                        zip -r -q /home/centos/cat/${configMap.component}.zip * -x "." -x ".git" -x "Jenkinsfile" -x "*.zip"
                        ls -ltr
                    """
                }
            }
            stage('publish artifact to nexus') {
                steps {
                    nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: "${nexus_url}",
                        groupId: 'com.useterraform',
                        version: "${versioncheck}",
                        repository: "${configMap.component}",
                        credentialsId: 'nexus-auth',
                        artifacts: [
                            [artifactId: "${configMap.component}",
                             classifier: '',
                             file: "/home/centos/cat/${configMap.component}.zip",
                             type: 'zip']
                        ]
                    )
                }
            }
            stage("${configMap.component}-deploy") {
                when { expression { return params.deploy } }
                steps {
                    build job: "eternal-project/eternal-apps/${configMap.component}-deploy", wait: true, parameters: [
                        string(name: 'version', value: "${versioncheck}"),
                        string(name: 'environment', value: "dev")
                    ]
                }
            }
        }
        post {
            always {
                echo "Check your status below: failure or success"
                deleteDir()
            }
            failure {
                echo "your build failed"
            }
            success {
                echo "your build is successful, thumbs up"
            }
        }
    }
}
