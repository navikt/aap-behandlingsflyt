package no.nav.aap.behandlingsflyt.server.toggles

import no.nav.aap.komponenter.httpklient.configForKey

object FeatureToggle {

    private val toggles = HashMap<String, Boolean>()

    fun erAktivert(key: String, default: Boolean): Boolean {
        if (toggles.containsKey(key)) {
            return toggles.getValue(key)
        }

        val configForKey = configForKey(key)
        if (configForKey != null) {
            return configForKey.toBoolean()
        }
        return default
    }

    fun aktiver(key: String, value: Boolean) {
        toggles[key] = value
    }
}