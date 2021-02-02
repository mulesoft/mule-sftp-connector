properties([
        parameters([
                string(name: 'muleVersion', defaultValue: 'latest',
                        description: 'Mule Runtime version to execute tests.')
        ])
])

Map pipelineParams = [
    "mavenAdditionalArgs" : "-Dtita.testing=${params.muleVersion}"
]

runtimeBuild(pipelineParams)