package no.nav.aap.behandlingsflyt.unleash

interface FeatureToggle {
    fun key(): String
}

enum class BehandlingsflytFeature : FeatureToggle {
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    IngenValidering,
    NyBrevbyggerV3,
    BrevtyperTilNyBrevbygger,
    Under18,
    VisIkkeRelevantPeriode,
    MigrerStansOgOpphor,
    SamordningFaktagrunnlagBrev,
    GReguleringUtplukkJobb,
    StudentV2,
    BackfillStansOpphor,
    BackfillSakstatusDatadeling,
    VentStatusForTilbakekrevingIBehandlingsflyt,
    MotorV2,
    IkkeSjekkInformasjonskravLovvalgMedlemsskapGrunnlag,
    GenererVilkarsvurderingOppsummeringPDF,

    // --- Krav ---
    KravSteg, // Visning
    LagreVurderRettighetsperiodeSomKrav, // Double write
    KravAutomatiskVurdering, // Double write
    KravManuellVurdering,
    NyttKravPeriodiserteAvklaringsbehov,

    // ------
    Avslag11_27,
    SkalViseAlleSykdomssteg,
    MeldeperiodeTilMeldekortBackendBasertPaaGjeldendeYtelsesbehandling,
    HoppOverKvalitetssikringVedIngenEndring,
    BosattStatsborgerskapGjennomslipp
    ;

    override fun key(): String = name
}