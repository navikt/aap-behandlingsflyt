package no.nav.aap.behandlingsflyt.unleash

interface FeatureToggle {
    fun key(): String
}

enum class BehandlingsflytFeature : FeatureToggle {
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    IngenValidering,
    NyBrevbyggerV3,
    Under18,
    TrekkSoeknadOpprettetFraLegeerklaering,
    KvalitetssikringVed2213,
    VisIkkeRelevantPeriode,
    MigrerStansOgOpphor,
    AvslagLovvalgMedlemskap,
    SamordningFaktagrunnlagBrev,
    GReguleringUtplukkJobb,
    ForutgaaendeGap,
    FjernTilbakefoeringTransisjon,
    MaritimtArbeid,
    ForstegangsbehandlingEtterAvslagOppgave,
    AlleEndringerKreverKvalitetssikring,
    RevurderingEtterAvslagSkalKvalitetssikres,
    MeldepliktForsteFraForsteInnvilgelse,
    MeldekortEndretAvSaksbehandler,
    AutomatiskStans1118,
    ;

    override fun key(): String = name
}