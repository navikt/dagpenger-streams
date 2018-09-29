pipeline {
  agent any

  stages {
    stage('Install dependencies') {
      steps {
        sh "./gradlew assemble"
      }
    }

    stage('Build') {
      steps {
        sh "./gradlew check"
      }
    }

    stage("Publish") {
      when {
        branch "master"
      }

      steps {
        archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true

        withCredentials([usernamePassword(credentialsId: 'repo.adeo.no', usernameVariable: 'REPO_CREDENTIAL_USR', passwordVariable: 'REPO_CREDENTIAL_PSW')]) {
          sh "./gradlew showVersion"
          sh "git tag -l"
          sh "./gradlew -PmavenUser=${env.REPO_CREDENTIAL_USR} -PmavenPassword=${env.REPO_CREDENTIAL_PSW} publish"
        }
      }
    }
  }
}
