# Kodeskjeletter

Tilpass navnene, men behold mønstrene. Pakkenavn følger eksisterende struktur (eller en vertikal feature-pakke som
`no.nav.aap.bistandsbehov`). Hvert kodeskjelett viser til eksempler fra koden. Åpne et eksempel før du skriver ditt eget.

## Steg (eksempler: `TjenestepensjonRefusjonskravSteg`, `VurderSykdomSteg` (periodisert), `UnderveisSteg` (beregning))

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

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {   // kjøres ved HVER gjennomgang: idempotent
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.NY_VURDERING,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },   // «skal vedtaket inneholde en menneskelig vurdering?»
            erTilstrekkeligVurdert = { nyVurderingRepository.hentHvisEksisterer(kontekst.behandlingId) != null },
            tilbakestillGrunnlag = {                                          // tilbake til vurderingen fra forrige behandling
                val forrige = kontekst.forrigeBehandlingId?.let { nyVurderingRepository.hentHvisEksisterer(it) }
                nyVurderingRepository.lagre(kontekst.behandlingId, forrige)
            },
            kontekst = kontekst,
        )
        return Fullført
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean =
        when (kontekst.vurderingType) {                                       // uttømmende med vilje
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
Registrering i flyten (`Revurdering.kt`): `.medSteg(steg = NyVurderingSteg, vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.MOTTATT_SØKNAD, Vurderingsbehov.HELHETLIG_VURDERING, Vurderingsbehov.NY_VURDERING), informasjonskrav = emptyList())`.

## Løsning + løser (eksempler: `RefusjonkravLøsning`/`RefusjonkravLøser`; periodisert: `AvklarSykdomLøsning`/`AvklarSykdomLøser`)

```kotlin
// behandling/avklaringsbehov/løsning/NyVurderingLøsning.kt   (sealed-hierarki: må ligge i denne pakken)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = NY_VURDERING_KODE)                       // const fra kontrakt AvklaringsbehovKode.kt
class NyVurderingLøsning(
    @param:JsonProperty("vurdering", required = true) val vurdering: NyVurderingDto,
    @param:JsonProperty("behovstype", required = true, defaultValue = NY_VURDERING_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`50xx`,
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider) =
        NyVurderingLøser(repositoryProvider).løs(kontekst, this)
}

// behandling/avklaringsbehov/løser/NyVurderingLøser.kt       (sealed interface AvklaringsbehovsLøser: samme pakke)
class NyVurderingLøser(private val repository: NyVurderingRepository) : AvklaringsbehovsLøser<NyVurderingLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(repository = repositoryProvider.provide())

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: NyVurderingLøsning): LøsningsResultat {
        val vurdering = løsning.vurdering.tilDomene(vurdertAv = kontekst.bruker)   // kast UgyldigForespørselException ved ugyldige data
        repository.lagre(kontekst.behandlingId(), vurdering)
        return LøsningsResultat(begrunnelse = vurdering.begrunnelse)              // kreverToTrinn = true tvinger fram beslutter
    }

    override fun forBehov(): Definisjon = Definisjon.NY_VURDERING
}
```

## Informasjonskrav (eksempler: `YrkesskadeInformasjonskrav`, `InntektInformasjonskrav`, `MeldekortInformasjonskrav`)

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

    override fun klargjør(kontekst: FlytKontekstMedPerioder) =             // DB tillatt, IKKE HTTP
        Input(sakService.hent(kontekst.sakId).person.aktivIdent().identifikator, kontekst.rettighetsperiode)

    override fun hentData(input: Input) = Registerdata(gateway.hent(input.ident, input.periode))   // HTTP tillatt, IKKE DB (parallelt)

    override fun oppdater(input: Input, registerdata: Registerdata, kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val gammel = repository.hentHvisEksisterer(kontekst.behandlingId)  // DB tillatt, IKKE HTTP
        if (gammel?.data == registerdata.data) return Informasjonskrav.Endret.IKKE_ENDRET
        repository.lagre(kontekst.behandlingId, registerdata.data)
        return Informasjonskrav.Endret.ENDRET                             // ENDRET → gå tilbake hvis dette steget ligger før det aktive steget
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.NYTT                     // ny enum-verdi; lagres
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            NyttInformasjonskrav(SakService(repositoryProvider, gatewayProvider), repositoryProvider.provide(),
                gatewayProvider.provide(), TidligereVurderingerImpl(repositoryProvider, gatewayProvider))
    }
}
```

## Repository (eksempler: `TjenestepensjonRefusjonskravVurderingRepositoryImpl`, `SykdomRepositoryImpl`)

Domene-modul: `interface NyVurderingRepository : no.nav.aap.lookup.repository.Repository { hentHvisEksisterer; lagre }`.
Repository-modul:
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
        }   // child-rader er uforanderlige: bruk alltid INSERT, aldri UPDATE (de deles etter kopier)
        connection.execute("INSERT INTO NY_VURDERING_GRUNNLAG (BEHANDLING_ID, NY_VURDERING_ID, AKTIV) VALUES (?, ?, TRUE)") {
            setParams { setLong(1, behandlingId.toLong()); setLong(2, vurderingId) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {   // ny grunnlagsrad, SAMME child-rad
        connection.execute("""
            INSERT INTO NY_VURDERING_GRUNNLAG (BEHANDLING_ID, NY_VURDERING_ID, AKTIV)
            SELECT ?, NY_VURDERING_ID, TRUE FROM NY_VURDERING_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()) { setParams { setLong(1, tilBehandling.toLong()); setLong(2, fraBehandling.toLong()) } }
    }

    override fun slett(behandlingId: BehandlingId) { /* slett grunnlaget og child-radene til denne førstegangsbehandlingen permanent; idempotent */ }
    // hentHvisEksisterer: SELECT … FROM NY_VURDERING_GRUNNLAG g JOIN NY_VURDERING v ON … WHERE g.BEHANDLING_ID = ? AND g.AKTIV
}
```
Registrer deretter `.register<NyVurderingRepositoryImpl>()` i `PostgresRepositoryRegistry.kt`. Migrering:
`repository/src/main/resources/flyway/V1.<next>__ny_vurdering.sql` (neste nummer: `repo.py neste-migrering`).

## Grunnlag-API (eksempel: `api/src/main/kotlin/no/nav/aap/bistandsbehov/BistandsgrunnlagApi.kt`)

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
Registrering: `nyVurderingGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)` inne i `apiRouting { … }` i `App.kt`.

## Vilkår (ny stil; eksempler: `OvergangArbeidVilkår`, `Bistandsvilkåret`)

```kotlin
object NyttVilkår : Vilkårsvurderer<NyttFaktagrunnlag> {
    override val vilkårtype = Vilkårtype.NYTT_VILKÅR
    override fun vurder(faktagrunnlag: NyttFaktagrunnlag): Tidslinje<Vilkårsvurdering> =   // REN: ingen DB, ingen toggles
        faktagrunnlag.vurderinger.mapValue { v ->
            Vilkårsvurdering(utfall = if (v.oppfylt) Utfall.OPPFYLT else Utfall.IKKE_OPPFYLT,
                avslagsårsak = Avslagsårsak.X.takeUnless { v.oppfylt }, begrunnelse = v.begrunnelse,
                faktagrunnlag = faktagrunnlag, manuellVurdering = true)
        }
}
// i steget: VilkårService(repositoryProvider).vurderVilkår(kontekst.behandlingId, faktagrunnlag, NyttVilkår)
```
