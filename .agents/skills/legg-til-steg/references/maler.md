# Code skeletons

Adapt names; keep the patterns. Package names follow the existing layout (or a vertical feature package like
`no.nav.aap.bistandsbehov`). Real exemplars are listed per skeleton — open one before writing your own.

## Step (exemplars: `TjenestepensjonRefusjonskravSteg`, `VurderSykdomSteg` (periodised), `UnderveisSteg` (computation))

```kotlin
package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class NyVurderingSteg private constructor(
    private val nyVurderingRepository: NyVurderingRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        nyVurderingRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {   // runs on EVERY pass: idempotent
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.NY_VURDERING,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },   // "will the vedtak contain a human vurdering?"
            erTilstrekkeligVurdert = { nyVurderingRepository.hentHvisEksisterer(kontekst.behandlingId) != null },
            tilbakestillGrunnlag = {                                          // back to forrige behandling's vurdering
                val forrige = kontekst.forrigeBehandlingId?.let { nyVurderingRepository.hentHvisEksisterer(it) }
                nyVurderingRepository.lagre(kontekst.behandlingId, forrige)
            },
            kontekst = kontekst,
        )
        return Fullført
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean =
        when (kontekst.vurderingType) {                                       // exhaustive on purpose
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING,
            VurderingType.MIGERING_FRA_ARENA -> when {
                tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                else -> kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty()
                        || nyVurderingRepository.hentHvisEksisterer(kontekst.behandlingId) != null
            }

            VurderingType.MELDEKORT,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.MIGRER_RETTIGHETSPERIODE,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.G_REGULERING,
            VurderingType.OVERGANG_UFORE_STANS,
            VurderingType.IKKE_RELEVANT -> false
        }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg =
            NyVurderingSteg(repositoryProvider, gatewayProvider)

        override fun type(): StegType = StegType.NY_VURDERING
    }
}
```
Flow registration (`Revurdering.kt`): `.medSteg(steg = NyVurderingSteg, vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.MOTTATT_SØKNAD, Vurderingsbehov.HELHETLIG_VURDERING, Vurderingsbehov.NY_VURDERING), informasjonskrav = emptyList())`.

## Løsning + løser (exemplars: `RefusjonkravLøsning`/`RefusjonkravLøser`; periodised: `AvklarSykdomLøsning`/`AvklarSykdomLøser`)

```kotlin
// behandling/avklaringsbehov/løsning/NyVurderingLøsning.kt   (sealed hierarchy: must be in this package)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = NY_VURDERING_KODE)                       // const from kontrakt AvklaringsbehovKode.kt
class NyVurderingLøsning(
    @param:JsonProperty("vurdering", required = true) val vurdering: NyVurderingDto,
    @param:JsonProperty("behovstype", required = true, defaultValue = NY_VURDERING_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`50xx`,
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider) =
        NyVurderingLøser(repositoryProvider).løs(kontekst, this)
}

// behandling/avklaringsbehov/løser/NyVurderingLøser.kt       (sealed interface AvklaringsbehovsLøser: same package)
class NyVurderingLøser(private val repository: NyVurderingRepository) : AvklaringsbehovsLøser<NyVurderingLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(repository = repositoryProvider.provide())

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: NyVurderingLøsning): LøsningsResultat {
        val vurdering = løsning.vurdering.tilDomene(vurdertAv = kontekst.bruker)   // throw UgyldigForespørselException on bad input
        repository.lagre(kontekst.behandlingId(), vurdering)
        return LøsningsResultat(begrunnelse = vurdering.begrunnelse)              // kreverToTrinn = true to force beslutter
    }

    override fun forBehov(): Definisjon = Definisjon.NY_VURDERING
}
```

## Informasjonskrav (exemplars: `YrkesskadeInformasjonskrav`, `InntektInformasjonskrav`, `MeldekortInformasjonskrav`)

```kotlin
class NyttInformasjonskrav private constructor(
    private val sakService: SakService,
    private val repository: NyttGrunnlagRepository,
    private val gateway: NyGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav<NyttInformasjonskrav.Input, NyttInformasjonskrav.Registerdata> {
    override val navn = Companion.navn

    data class Input(val ident: String, val periode: Periode) : InformasjonskravInput
    data class Registerdata(val data: List<NyData>) : InformasjonskravRegisterdata

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?) =
        kontekst.erFørstegangsbehandlingEllerRevurdering()
            && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
            && (oppdatert.ikkeKjørtSisteKalenderdag() || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode)

    override fun klargjør(kontekst: FlytKontekstMedPerioder) =             // DB ok, NO http
        Input(sakService.hent(kontekst.sakId).person.aktivIdent().identifikator, kontekst.rettighetsperiode)

    override fun hentData(input: Input) = Registerdata(gateway.hent(input.ident, input.periode))   // http ok, NO DB (parallel)

    override fun oppdater(input: Input, registerdata: Registerdata, kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val gammel = repository.hentHvisEksisterer(kontekst.behandlingId)  // DB ok, NO http
        if (gammel?.data == registerdata.data) return Informasjonskrav.Endret.IKKE_ENDRET
        repository.lagre(kontekst.behandlingId, registerdata.data)
        return Informasjonskrav.Endret.ENDRET                             // ENDRET → rewind if this step is behind the active one
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.NYTT                     // new enum value; persisted
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            NyttInformasjonskrav(SakService(repositoryProvider, gatewayProvider), repositoryProvider.provide(),
                gatewayProvider.provide(), TidligereVurderingerImpl(repositoryProvider, gatewayProvider))
    }
}
```

## Repository (exemplar: `TjenestepensjonRefusjonskravVurderingRepositoryImpl`, `SykdomRepositoryImpl`)

Domain module: `interface NyVurderingRepository : no.nav.aap.lookup.repository.Repository { hentHvisEksisterer; lagre }`.
Repository module:
```kotlin
class NyVurderingRepositoryImpl(private val connection: DBConnection) : NyVurderingRepository {
    companion object : Factory<NyVurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection) = NyVurderingRepositoryImpl(connection)
    }

    override fun lagre(behandlingId: BehandlingId, vurdering: NyVurdering?) {
        connection.execute("UPDATE NY_VURDERING_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams { setLong(1, behandlingId.toLong()) }
        }
        if (vurdering == null) return
        val vurderingId = connection.executeReturnKey("INSERT INTO NY_VURDERING (BEGRUNNELSE, VURDERT_AV) VALUES (?, ?)") {
            setParams { setString(1, vurdering.begrunnelse); setString(2, vurdering.vurdertAv.ident) }
        }   // child rows are immutable: always INSERT, never UPDATE (they are shared after kopier)
        connection.execute("INSERT INTO NY_VURDERING_GRUNNLAG (BEHANDLING_ID, NY_VURDERING_ID, AKTIV) VALUES (?, ?, TRUE)") {
            setParams { setLong(1, behandlingId.toLong()); setLong(2, vurderingId) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {   // new grunnlag row, SAME child row
        connection.execute("""
            INSERT INTO NY_VURDERING_GRUNNLAG (BEHANDLING_ID, NY_VURDERING_ID, AKTIV)
            SELECT ?, NY_VURDERING_ID, TRUE FROM NY_VURDERING_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()) { setParams { setLong(1, tilBehandling.toLong()); setLong(2, fraBehandling.toLong()) } }
    }

    override fun slett(behandlingId: BehandlingId) { /* hard delete grunnlag + child rows of this (førstegangs)behandling; idempotent */ }
    // hentHvisEksisterer: SELECT … FROM NY_VURDERING_GRUNNLAG g JOIN NY_VURDERING v ON … WHERE g.BEHANDLING_ID = ? AND g.AKTIV
}
```
Then `.register<NyVurderingRepositoryImpl>()` in `PostgresRepositoryRegistry.kt`. Migration
`repository/src/main/resources/flyway/V1.<next>__ny_vurdering.sql` (next number: `repo.py neste-migrering`).

## Grunnlag API (exemplar: `api/src/main/kotlin/no/nav/aap/bistandsbehov/BistandsgrunnlagApi.kt`)

```kotlin
fun NormalOpenAPIRoute.nyVurderingGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry, gatewayProvider: GatewayProvider) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/ny-vurdering") {
            getGrunnlag<BehandlingReferanse, NyVurderingGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                påkrevdRolle = Definisjon.NY_VURDERING.løsesAv,
            ) { req ->
                val respons = dataSource.transaction(readOnly = true) { connection ->
                    val provider = repositoryRegistry.provider(connection)
                    val behandling = BehandlingReferanseService(provider.provide()).behandling(req)
                    NyVurderingGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = provider.provide<NyVurderingRepository>().hentHvisEksisterer(behandling.id)?.tilResponse(),
                    )
                }
                respond(respons)
            }
        }
    }
}
```
Register: `nyVurderingGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)` inside `apiRouting { … }` in `App.kt`.

## Vilkår (new style; exemplars: `OvergangArbeidVilkår`, `Bistandsvilkåret`)

```kotlin
object NyttVilkår : Vilkårsvurderer<NyttFaktagrunnlag> {
    override val vilkårtype = Vilkårtype.NYTT_VILKÅR
    override fun vurder(faktagrunnlag: NyttFaktagrunnlag): Tidslinje<Vilkårsvurdering> =   // PURE: no DB, no toggles
        faktagrunnlag.vurderinger.mapValue { v ->
            Vilkårsvurdering(utfall = if (v.oppfylt) Utfall.OPPFYLT else Utfall.IKKE_OPPFYLT,
                avslagsårsak = Avslagsårsak.X.takeUnless { v.oppfylt }, begrunnelse = v.begrunnelse,
                faktagrunnlag = faktagrunnlag, manuellVurdering = true)
        }
}
// in the step: VilkårService(repositoryProvider).vurderVilkår(kontekst.behandlingId, faktagrunnlag, NyttVilkår)
```
