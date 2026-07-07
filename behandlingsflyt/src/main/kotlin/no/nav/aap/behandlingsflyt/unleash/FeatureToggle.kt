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
    VisIkkeRelevantPeriode,
    MigrerStansOgOpphor,
    SamordningFaktagrunnlagBrev,
    GReguleringUtplukkJobb,
    RevurderingEtterAvslagSkalKvalitetssikres,
    MeldekortEndretAvSaksbehandler,
    AutomatiskStans1118,
    StudentV2,
    BackfillStansOpphor,
    VentStatusForTilbakekrevingIBehandlingsflyt,
    // --- Krav ---
    KravSteg, // Visning
    LagreVurderRettighetsperiodeSomKrav, // Double write
    KravAutomatiskVurdering, // Double write
    KravManuellVurdering,
    NyttKravPeriodiserteAvklaringsbehov,
    // ------
    OppfoelgingsoppgaveSynligMedEnGang,
    ManuellInntektDelvisUfore,
    Avslag11_27
    ;

    override fun key(): String = name
}