package no.nav.aap.behandlingsflyt.unleash

interface FeatureToggle {
    fun key(): String
}

enum class BehandlingsflytFeature : FeatureToggle {
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    IngenValidering,
    NyBrevbyggerV3,
    LagreVedtakIFatteVedtak,
    Under18,
    SendBrevVedMottattKlage,
    ForlengelseIManuellBehandling,
    InstitusjonsoppholdJobb,
    TrekkSoeknadOpprettetFraLegeerklaering,
    VirksomhetsEtablering,
    KvalitetssikringVed2213,
    tilbakekrevingsOppgaverTilOppgave,
    HentingAvInstitusjonsOpphold,
    VisIkkeRelevantPeriode,
    NyTidligereVurderinger,
    BekreftVurderingerOppfolging,
    LagreStansOgOpphor,
    SamordningBarnepensjon,
    RefusjonkravIRevurdering
    ;

    override fun key(): String = name
}