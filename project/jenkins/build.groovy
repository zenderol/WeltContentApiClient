node('slave') {
    stage name: 'Git'

    checkout([$class                           : 'GitSCM',
              branches                         : [[name: '*/master']],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[
                                                          url: 'git@github.com:WeltN24/WeltContentApiClient.git'
                                                  ]]
    ])

    init = load 'project/jenkins/init.groovy'
    init.setup()

    test = load 'project/jenkins/test.groovy'
    test.doTest()

    // todo: publish

}
