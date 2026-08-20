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
    StudentV2,
    BackfillStansOpphor,
    VentStatusForTilbakekrevingIBehandlingsflyt,
    MotorV2,
    IngenStoppHendelseVedAtomaerBehandling,
    IkkeSjekkInformasjonskravLovvalgMedlemsskapGrunnlag,
    GenererVilkarsvurderingOppsummeringPDF,
    MigreringHentArenaGrunnlag,

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
    HoppOverKvalitetssikringVedIngenEndring
    ;

    override fun key(): String = name
}