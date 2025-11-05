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
    NyBrevtype11_17,
    NyBrevtype11_18,
    Aktivitetsplikt11_9,
    OverforingsdatoNullForAvregning,
    NyeSykdomVilkar,
    OvergangArbeid,
    AvklaringsbehovService,
    AvklaringsbehovServiceFormkrav,
    LovvalgMedlemskapPeriodisertMigrering,
    LovvalgMedlemskapPeriodisert,
    KvalitetssikringsSteg,
    FatteVedtakAvklaringsbehovService,
    SykdomPeriodisertMigrering,
    ;

    override fun key(): String = name
}


