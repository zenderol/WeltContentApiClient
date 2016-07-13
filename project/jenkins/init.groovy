def setup() {

    stage name: 'Env Setup'

    sh 'git rev-parse --short HEAD > git_commit.txt'
    env.GIT_COMMIT = readFile('git_commit.txt')
    env.GIT_BRANCH = 'master'
    env.INITIAL_NODE_NAME = env.NODE_NAME

    stage name: 'Credential Setup Bintray'

    withCredentials([[$class: 'StringBinding', credentialsId: 'BINTRAY_API_KEY_CI_WELTN24', variable: 'BINTRAY_API_KEY_CI_WELTN24']]) {
        sh """
            rm -f ~/.bintray/.credentials || true
            mkdir -p ~/.bintray
            touch ~/.bintray/.credentials
            echo -e 'realm = Bintray API Realm' >> ~/.bintray/.credentials
            echo -e 'host = api.bintray.com' >> ~/.bintray/.credentials
            echo -e 'user = ci-weltn24' >> ~/.bintray/.credentials
            echo -n 'password = ' >> ~/.bintray/.credentials
        """
        sh "echo -e  " + env.BINTRAY_API_KEY_CI_WELTN24 + " >> ~/.bintray/.credentials"
    }

}

return this
