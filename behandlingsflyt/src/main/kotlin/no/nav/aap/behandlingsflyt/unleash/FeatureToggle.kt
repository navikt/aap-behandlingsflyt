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
    InstitusjonsoppholdJobb,
    TrekkSoeknadOpprettetFraLegeerklaering,
    KvalitetssikringVed2213,
    tilbakekrevingsOppgaverTilOppgave,
    HentingAvInstitusjonsOpphold,
    VisIkkeRelevantPeriode,
    BekreftVurderingerOppfolging,
    LagreStansOgOpphor,
    MigrerStansOgOpphor,
    SamordningBarnepensjon,
    RefusjonkravIRevurdering,
    SignaturEnhetFraOppgave,
    VedtakslengdeAvklaringsbehov,
    hentDagpengerPerioder,
    UtvidVedtakslengdeUnderEttAr,
    OpprettManuellVedtakslengdeBehandling,
    ;

    override fun key(): String = name
}