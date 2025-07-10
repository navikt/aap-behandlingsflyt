package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.Motor
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import javax.sql.DataSource


class MotorExtension : BeforeAllCallback, AfterAllCallback, ParameterResolver {
    override fun beforeAll(context: ExtensionContext?) {
        val motor = motor(context!!)
        if (!motor.kjører()) {
            try {
                motor.start()
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        motor(context!!).stop()
    }

    override fun supportsParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?
    ): Boolean {
        return parameterContext?.parameter?.type?.equals(DataSource::class.java) ?: false
    }

    override fun resolveParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?
    ): Any? {
        return dataSource(extensionContext!!)
    }

    private fun dataSource(extensionContext: ExtensionContext): DataSource {
        val rootContext = extensionContext.root
        val store = rootContext.getStore(ExtensionContext.Namespace.GLOBAL)

        return store.getOrComputeIfAbsent("ds") {
            InitTestDatabase.freshDatabase()
        } as DataSource
    }

    private fun motor(extensionContext: ExtensionContext): Motor {
        val rootContext = extensionContext.root
        val store = rootContext.getStore(ExtensionContext.Namespace.GLOBAL)

        return store.getOrComputeIfAbsent("motor") {
            val ds = dataSource(extensionContext)
            Motor(ds, 8, jobber = ProsesseringsJobber.alle(), repositoryRegistry = postgresRepositoryRegistry)
        } as Motor
    }
}