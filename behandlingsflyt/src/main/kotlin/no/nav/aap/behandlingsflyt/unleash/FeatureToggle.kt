package no.nav.aap.behandlingsflyt.unleash

interface FeatureToggle {
    fun key(): String

    /** Hva feature skal være, hvis unleash er utilgjengelig. */
    val default: Boolean
}

enum class BehandlingsflytFeature(
    override val default: Boolean = false,
): FeatureToggle {
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    IngenValidering,
    NyBrevbyggerV3,
    BrevtyperTilNyBrevbygger,
    Under18,
    VisIkkeRelevantPeriode,
    MigrerStansOgOpphor,
    SamordningFaktagrunnlagBrev,
    GReguleringUtplukkJobb,
    BackfillStansOpphor,
    VentStatusForTilbakekrevingIBehandlingsflyt,
    IkkeSjekkInformasjonskravLovvalgMedlemsskapGrunnlag,
    GenererVilkarsvurderingOppsummeringPDF,

    // --- Krav ---
    BackfillKrav,
    KravSteg, // Visning
    LagreVurderRettighetsperiodeSomKrav, // Double write
    KravAutomatiskVurdering, // Double write
    KravManuellVurdering,
    NyttKravPeriodiserteAvklaringsbehov,

    // ------
    Avslag11_27,
    SkalViseAlleSykdomssteg,
    MeldeperiodeTilMeldekortBackendBasertPaaGjeldendeYtelsesbehandling,
    BosattStatsborgerskapGjennomslipp,
    HoppOverBeslutterVedAvslagSykdom
    ;

    override fun key(): String = name
}
