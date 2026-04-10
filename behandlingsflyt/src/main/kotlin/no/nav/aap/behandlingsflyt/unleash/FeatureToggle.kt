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
    InstitusjonsoppholdJobb,
    TrekkSoeknadOpprettetFraLegeerklaering,
    KvalitetssikringVed2213,
    VisIkkeRelevantPeriode,
    BekreftVurderingerOppfolging,
    LagreStansOgOpphor,
    MigrerStansOgOpphor,
    SamordningBarnepensjon,
    SignaturEnhetFraOppgave,
    VedtakslengdeAvklaringsbehov,
    hentDagpengerPerioder,
    hentTiltakspengerPerioder,
    UtvidVedtakslengdeUnderEttAr,
    OpprettManuellVedtakslengdeBehandling,
    ;

    override fun key(): String = name
}