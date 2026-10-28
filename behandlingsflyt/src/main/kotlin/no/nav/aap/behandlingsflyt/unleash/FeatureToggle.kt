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
    BrevtyperTilNyBrevbygger,
    Under18,
    VisIkkeRelevantPeriode,
    MigrerStansOgOpphor,
    GReguleringUtplukkJobb,
    BackfillStansOpphor,
    VentStatusForTilbakekrevingIBehandlingsflyt,
    IkkeSjekkInformasjonskravLovvalgMedlemsskapGrunnlag,
    SynkroniserArenaMeldeperiodesyklus,
    DynamiskFrivilligSykestipend,

    // --- Krav ---
    KravSteg, // Visning
    KravManuellVurdering,
    NyttKravPeriodiserteAvklaringsbehov,

    // --- Migrering ---
    MigererSykdomFraArenaAutomatisk,
    MigrerRefusjonskravFraArenaAutomatisk,
    MigrererKravFraArenaAutomatisk,
    MigrererOppholdskravFraArenaAutomatisk,
    MigrererVedtakslengdeFraArenaAutomatisk,
    MigrererBeregningstidspunktFraArenaAutomatisk,

    // --- Utbetaling ---
    FjerneAvventUtbetalingForSamordningArbeidsgiver,

    // ------
    Avslag11_27,
    MeldeperiodeTilMeldekortBackendBasertPaaGjeldendeYtelsesbehandling,
    BosattStatsborgerskapGjennomslipp,
    HoppOverBeslutterVedAvslagSykdom,
    KanVurdereRefusjonIRevurdering,
    KunEnAktivKlagebehandling,
    KlagePaaTilbakekreving,
    NySoknadTilApiIntern,
    SammenhengendeInstitusjonsopphold,
    DatadelingBehandlingVisning,
    Helseinstitusjon1Manedsregel,
    BackfillRedusertDagsats,
    ;

    override fun key(): String = name
}
