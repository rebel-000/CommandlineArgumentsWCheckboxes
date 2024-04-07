package com.github.rebel000.cmdlineargs

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(name = "com.github.rebel000.cmdlineargs.sharedargs", storages = [Storage("CommandlineArgs.xml", roamingType = RoamingType.DEFAULT)])
class SharedArgsStorage : SimplePersistentStateComponent<SharedArgsStorage.SharedArgsState>(SharedArgsState()) {
    class SharedArgsState : BaseState() {
        var sharedArgs by string()
    }
}