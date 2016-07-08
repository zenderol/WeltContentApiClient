node('slave') {

    withCredentials([[$class: 'StringBinding', credentialsId: 'GITHUB_ACCESS_TOKEN', variable: 'GITHUB_ACCESS_TOKEN']]) {


        env.ghprbActualCommit = "${ghprbActualCommit}"
        env.sha1 = "${sha1}"

        // set status to pending, create pr-related files (commitMessage and pullRequestNumber)
        sh './project/jenkins/bashscripts/github_pr_status.sh pending'

        stage name: 'Git'

        checkout changelog: true, poll: false, scm: [
                $class                           : 'GitSCM',
                branches                         : [[name: "${env.sha1}"]],
                doGenerateSubmoduleConfigurations: false,
                extensions                       : [],
                submoduleCfg                     : [],
                userRemoteConfigs                : [[
                                                            refspec: '+refs/pull/*:refs/remotes/origin/pr/*',
                                                            url    : 'git@github.com:WeltN24/WeltContentApiClient.git'
                                                    ]]
        ]

        try {
            init = load 'project/jenkins/init.groovy'
            init.setup()

            test = load 'project/jenkins/test.groovy'
            test.doTest()

            // set status to success
            sh './project/jenkins/bashscripts/github_pr_status.sh success'
        } catch (all) {

            // set status to failure if there was an error
            sh './project/jenkins/bashscripts/github_pr_status.sh failure'
            throw all
        }

    }

}
