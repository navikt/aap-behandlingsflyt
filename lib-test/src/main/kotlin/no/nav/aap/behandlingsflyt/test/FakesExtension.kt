package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.ParameterizedType

internal class FakesExtension() : BeforeAllCallback, ParameterResolver,
    BeforeEachCallback {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
    }

    override fun beforeAll(context: ExtensionContext) {
        FakeServers.start()
    }

    override fun beforeEach(context: ExtensionContext) {
        FakeServers.statistikkHendelser.clear()
        FakeServers.legeerklæringStatuser.clear()
        FakePersoner.nullstillPersoner()
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean {
        val parameter = parameterContext.parameter

        val parameterizedType = parameter.parameterizedType
        if (parameterizedType is ParameterizedType) {
            return when (val firstParamType = parameterizedType.actualTypeArguments[0]) {
                is Class<*> -> {
                    firstParamType == StoppetBehandling::class.java
                }

                else -> {
                    return false
                }
            }
        }
        return false
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        if (parameterContext.parameter.type == List::class.java) {
            val parameterizedType = parameterContext.parameter.parameterizedType
            if (parameterizedType is ParameterizedType) {
                val firstArg = parameterizedType.actualTypeArguments[0]
                if (firstArg == StoppetBehandling::class.java) {
                    return FakeServers.statistikkHendelser
                }
            }
        }
        throw IllegalArgumentException("Not supported parameter type")
    }
}