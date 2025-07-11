package no.nav.aap.behandlingsflyt.test

import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import javax.sql.DataSource

class FreshDatabaseExtension : AfterAllCallback, ParameterResolver {
    companion object {
        val dataSource = InitTestDatabase.freshDatabase()
    }
    override fun afterAll(context: ExtensionContext?) {
//        (dataSource as Closeable).close()
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
        return dataSource
    }
}