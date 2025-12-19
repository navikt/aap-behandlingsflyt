package no.nav.aap.behandlingsflyt.unleash

interface FeatureToggle {
    fun key(): String
}

enum class BehandlingsflytFeature : FeatureToggle {
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    IngenValidering,
    NyBrevtype11_17,
    OverforingsdatoNullForAvregning,
    OvergangArbeid,
    KvalitetssikringsSteg,
    BedreUttrekkAvSakerMedFritakMeldeplikt,
    EOSBeregning,
    NyBrevbyggerV3,
    LagreVedtakIFatteVedtak,
    PeriodisertSykepengeErstatningNyAvklaringsbehovService,
    MidlertidigStopp11_4,
    Under18,
    ValiderOvergangUfore,
    KravOmInntektsbortfall,
    MigrerMeldepliktFritak,
    SosialRefusjon,
    HentSykepengerVedOverlapp,
    KanSendeBrevOmBarnetilleggSatsRegulering,
    PeriodisertSykdom,
    ;

    override fun key(): String = name
}
