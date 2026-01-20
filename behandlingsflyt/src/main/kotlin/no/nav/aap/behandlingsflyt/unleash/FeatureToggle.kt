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
    Under18,
    ValiderOvergangUfore,
    KravOmInntektsbortfall,
    MigrerMeldepliktFritak,
    SosialRefusjon,
    MigrerRettighetsperiode,
    HentSykepengerVedOverlapp,
    SendBrevVedMottattKlage,
    PeriodisertSykdom,
    Sykestipend,
    Forlengelse,
    ForlengelseIManuellBehandling,
    UtvidVedtakslengdeJobb,
    TrekkSoeknadOpprettetFraLegeerklaering,
    ;

    override fun key(): String = name
}