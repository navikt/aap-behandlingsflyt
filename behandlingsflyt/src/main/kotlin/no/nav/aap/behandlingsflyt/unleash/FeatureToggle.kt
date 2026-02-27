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
    Under18,
    SendBrevVedMottattKlage,
    ForlengelseIManuellBehandling,
    InstitusjonsoppholdJobb,
    TrekkSoeknadOpprettetFraLegeerklaering,
    UbehandledeMeldekortJobb,
    VirksomhetsEtablering,
    KvalitetssikringVed2213,
    tilbakekrevingsOppgaverTilOppgave,
    PeriodiseringHelseinstitusjonOpphold,
    HentingAvInstitusjonsOpphold,
    VisIkkeRelevantPeriode,
    NyTidligereVurderinger,
    MigrerInstitusjonsopphold,
    LagreStansOgOpphor,
    ;

    override fun key(): String = name
}