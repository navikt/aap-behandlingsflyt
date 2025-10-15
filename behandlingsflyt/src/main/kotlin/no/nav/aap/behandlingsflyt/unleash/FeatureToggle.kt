package no.nav.aap.behandlingsflyt.unleash

interface FeatureToggle {
    fun key(): String
}

enum class BehandlingsflytFeature : FeatureToggle {
    // Eksempel på feature toggle. Kan fjernes når det legges til nye.
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    DummyFeature,
    FritakMeldeplikt,
    IngenValidering,
    InnhentEnhetsregisterData,
    SendForvaltningsmelding,
    NyBrevtype11_18,
    Aktivitetsplikt11_7,
    Aktivitetsplikt11_9,
    TilgangssjekkSettPaaVent,
    OverforingsdatoNullForAvregning,
    NyeSykdomVilkar,
    OvergangUfore,
    IverksettUtbetalingSomSelvstendigJobb,
    RefaktorereFastsettSykdomsvilkar,
    SykepengerPeriodisert,
    OvergangArbeid,
    ;

    override fun key(): String = name
}


