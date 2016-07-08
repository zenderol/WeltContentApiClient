def setup() {

    stage name: 'Setup'

    sh 'git rev-parse --short HEAD > git_commit.txt'
    env.GIT_COMMIT = readFile('git_commit.txt')
    env.GIT_BRANCH = 'master'
    env.INITIAL_NODE_NAME = env.NODE_NAME

}

return this
