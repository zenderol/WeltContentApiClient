def doPublish() {
    stage name: 'Publish to bintray.com'
    wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
        sh './activator publish'
    }
}

return this