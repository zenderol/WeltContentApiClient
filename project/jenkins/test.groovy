def doTest() {
    stage name: 'Test'

    try {
        wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
            sh './activator scalastyle'
        }
    } finally {
        step([$class: 'CheckStylePublisher', pattern: '**/scalastyle-result.xml'])
    }
    try {
        wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
            sh './activator clean coverage test'
        }
    } finally {
        step([$class: 'JUnitResultArchiver', testResults: '**/target/test-reports/*.xml'])
    }
    try {
        wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
            sh './activator coverageReport'
            sh './activator coverageAggregate'
        }
    } finally {
        step([
                $class    : 'ScoveragePublisher',
                reportDir : 'target/scala-2.11/scoverage-report',
                reportFile: 'scoverage.xml'
        ])
    }
}

return this
