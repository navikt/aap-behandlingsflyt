package no.nav.aap.behandlingsflyt.unleash

interface FeatureToggle {
    fun key(): String
}

enum class BehandlingsflytFeature : FeatureToggle {
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    IngenValidering,
    NyBrevbyggerV3,
    Under18,
    InstitusjonsoppholdJobb,
    TrekkSoeknadOpprettetFraLegeerklaering,
    KvalitetssikringVed2213,
    VisIkkeRelevantPeriode,
    BekreftVurderingerOppfolging,
    MigrerStansOgOpphor,
    SamordningBarnepensjon,
    hentTiltakspengerPerioder,
    AvslagLovvalgMedlemskap,
    ForutgaaendeForbedringer
    ;

    override fun key(): String = name
}