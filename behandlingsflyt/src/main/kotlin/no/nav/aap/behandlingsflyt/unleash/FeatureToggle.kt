package no.nav.aap.behandlingsflyt.unleash

interface FeatureToggle {
    fun key(): String
}

enum class BehandlingsflytFeature : FeatureToggle {
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    IngenValidering,
    NyBrevtype11_17,
    OvergangArbeid,
    KvalitetssikringsSteg,
    NyBrevbyggerV3,
    LagreVedtakIFatteVedtak,
    PeriodisertSykepengeErstatningNyAvklaringsbehovService,
    Under18,
    ValiderOvergangUfore,
    MigrerMeldepliktFritak,
    SosialRefusjon,
    MigrerRettighetsperiode,
    HentSykepengerVedOverlapp,
    SendBrevVedMottattKlage,
    PeriodisertSykdom,
    Sykestipend,
    ForlengelseIManuellBehandling,
    UtvidVedtakslengdeJobb,
    InstitusjonsoppholdJobb,
    TrekkSoeknadOpprettetFraLegeerklaering,
    ArbeidssokerBrevMedFaktagrunnlag,
    UbehandledeMeldekortJobb,
    ForenkletKvote,
    ;

    override fun key(): String = name
}