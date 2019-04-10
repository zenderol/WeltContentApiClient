pipeline {
    agent { label 'large' }
    options { buildDiscarder(logRotator(numToKeepStr: '50')) }

    parameters {
        booleanParam(name: 'QUICK_DEPLOY', defaultValue: false, description: 'Skip scala: test and coverage')
    }

    stages {
        stage('Git') {
            steps {
                checkout scm
            }
        }

        stage('Check') {
            when { changeRequest() }
            steps {
                ansiColor('xterm') {
                    sh "./sbt clean test"
                }
            }
        }

        stage('Parallel') {
            when { allOf { anyOf { branch 'master'; branch 'ssm'}; expression { return !params.QUICK_DEPLOY } } }
            failFast true
            parallel {
                stage('Scala Style') {
                    steps {
                        ansiColor('xterm') {
                            sh './sbt scalastyle'
                        }
                        step([$class: 'CheckStylePublisher', pattern: '**/scalastyle-result.xml'])
                    }
                }
                stage('Test') {
                    steps {
                        ansiColor('xterm') {
                            sh './sbt clean coverage test'
                        }
                        junit '**/target/test-reports/*.xml'
                    }
                }
            }
        }

        stage('Coverage') {
            when { allOf { anyOf { branch 'master'; branch 'ssm'}; expression { return !params.QUICK_DEPLOY } } }
            steps {
                ansiColor('xterm') {
                    sh './sbt coverageReport'
                    sh './sbt coverageAggregate'
                }
                step([$class: 'ScoveragePublisher', reportDir: 'target/scala-2.12/scoverage-report', reportFile: 'scoverage.xml'])
            }
        }

        stage('Publish') {
            when { anyOf { branch 'master'; branch 'ssm'} }
            // provide BINTRAY_{USER,PASS} as of https://github.com/sbt/sbt-bintray/blob/master/notes/0.5.0.markdown
            environment { BINTRAY_USER = "ci-weltn24" }
            steps {
                withCredentials([[$class: 'StringBinding', credentialsId: 'BINTRAY_API_KEY_CI_WELTN24', variable: 'BINTRAY_PASS']]) {
                    ansiColor('xterm') {
                        sh './sbt publish'
                    }
                    slackSend channel: 'section-tool-2', message: "Successfully published a new WeltContentApiClient version: ${env.BUILD_URL}"
                }
            }
        }
    }
    post {
        failure {
            slackSend color: "danger", channel: 'section-tool-2', message: ":facepalm: Build failed: ${env.BUILD_URL}"
        }
    }
}
