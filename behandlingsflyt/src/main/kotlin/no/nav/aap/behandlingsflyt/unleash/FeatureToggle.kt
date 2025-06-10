package no.nav.aap.behandlingsflyt.unleash

interface FeatureToggle {
    fun key(): String
}

enum class BehandlingsflytFeature : FeatureToggle {
    // Eksempel på feature toggle. Kan fjernes når det legges til nye.
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    DummyFeature,
    FasttrackMeldekort,
    OverstyrStarttidspunkt,
    AvventUtbetaling,
    FjernAutomatiskOppdateringAvBarnetillegg,
    FritakMeldeplikt,
    InnhentEnhetsregisterData
    Samvarsling
    ;

    override fun key(): String = name
}


