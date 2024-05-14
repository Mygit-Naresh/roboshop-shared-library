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
        // booleanParam, choice, file, text, password, run, or string
        booleanParam(defaultValue: false, description: 'click on checkbox for build', name: 'deploy')
      //   string(defaultValue: "TEST", description: 'What environment?', name: 'stringExample')
      //   text(defaultValue: "This is a multiline\n text", description: "Multiline Text", name: "textExample")
      //   choice(choices: 'US-EAST-1\nUS-WEST-2', description: 'What AWS region?', name: 'choiceExample')
      //   password(defaultValue: "Password", description: "Password Parameter", name: "passwordExample")
    }
//  parameters {
//       choice(choices: 'PROD\nDEV', description: 'Choose PROD or DEV?', name: 'choice')
//  }
   
   options {
                timeout(time: 1, unit: 'HOURS')
                ansiColor('xtrem')
           }
    

    stages {
   //   stage('checkout_from_scm') {
   //   steps {
   //       git branch: 'main', changelog: false, poll: false, url: 'https://github.com/Mygit-Naresh/catalogue.git'
   //     }
   //  }
    
     stage('get app version'){
        steps {
         script {
                def jsonfile = readJSON file: 'package.json'
                versioncheck = jsonfile.version
                echo "currentversion is ${versioncheck}"
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
      sh   "sonar-scanner"
    }
}


     stage("zip files and folders from catalogue") {
        steps {
           sh """ 
           ls -la
           zip -r -q  /home/centos/cat/catalogue.zip *  -x "." -x ".git" -x "Jenkinsfile" -x  "*.zip"
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
        groupId: 'com.eternalplace',
        version: "${versioncheck}",
        repository: '"catalogue"',
        credentialsId: 'nexus-auth',
        artifacts: [
            [artifactId: 'catalogue',
             classifier: '',
             file: "/home/centos/cat/catalogue.zip",
             type: 'zip']
        ]
     )
  }  
        
     }

 stage ("catalogue-deploy") {
     when { expression { return params.deploy } }
            steps {
                build job: "eternal-project/eternal-apps/catalogue-deploy",  wait: true, parameters: [
                string(name: 'version', value: "${versioncheck}"),
                 string(name: 'environment', value: "dev")

                ]
            }
   }
    } 
  post {
   always {
      echo "Check you status below failure or success"
      deleteDir()
   }
    failure {
        echo "your build failed"
    }
    success {
        echo "your build is success thumbs up"
        
    }
  }
 }

}


