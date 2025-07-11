package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.Motor
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class MotorExtension : BeforeAllCallback, AfterAllCallback, ParameterResolver {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun beforeAll(context: ExtensionContext?) {
        val motor = motor(context!!)
        if (!motor.kjører()) {
            startMotor(motor)
        }
    }

    fun startMotor(motor: Motor, antallForsøk: Int = 5, sleepInMs: Long = 1000) {
        try {
            motor.start()
        } catch (e: Exception) {
            log.error("Kunne ikke starte motor", e)
            Thread.sleep(sleepInMs)

            if (antallForsøk == 0) {
                throw e
            } else {
                startMotor(motor, antallForsøk - 1, sleepInMs)
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