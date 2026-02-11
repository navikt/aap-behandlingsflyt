package no.nav.aap.behandlingsflyt.unleash

interface FeatureToggle {
    fun key(): String
}

enum class BehandlingsflytFeature : FeatureToggle {
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    IngenValidering,
    NyBrevtype11_17,
    NyBrevbyggerV3,
    LagreVedtakIFatteVedtak,
    PeriodisertSykepengeErstatningNyAvklaringsbehovService,
    Under18,
    ValiderOvergangUfore,
    MigrerMeldepliktFritak,
    SosialRefusjon,
    MigrerRettighetsperiode,
    SendBrevVedMottattKlage,
    PeriodisertSykdom,
    ForlengelseIManuellBehandling,
    UtvidVedtakslengdeJobb,
    InstitusjonsoppholdJobb,
    TrekkSoeknadOpprettetFraLegeerklaering,
    UbehandledeMeldekortJobb,
    ForenkletKvote,
    PapirMeldekortFraBehandingsflyt,
    RettighetstypeSteg,
    VirksomhetsEtablering,
    KvalitetssikringVed2213,
    tilbakekrevingsOppgaverTilOppgave
    ;

    override fun key(): String = name
}