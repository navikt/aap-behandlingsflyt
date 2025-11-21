package no.nav.aap.behandlingsflyt.prosessering.statistikk

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.IKlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageResultatType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.AvsluttetBehandlingDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.BeregningsgrunnlagDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Diagnoser
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Grunnlag11_19DTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.GrunnlagUføreDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.GrunnlagYrkesskadeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.MeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetstypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelseDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelsePeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.UføreType
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsResultatDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype
import no.nav.aap.behandlingsflyt.pip.PipRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class StatistikkMetoder(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakService: SakService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val pipRepository: PipRepository,
    private val dokumentRepository: MottattDokumentRepository,
    private val sykdomRepository: SykdomRepository,
    private val underveisRepository: UnderveisRepository,
    private val meldekortRepository: MeldekortRepository,
    private val påklagetBehandlingRepository: PåklagetBehandlingRepository,
    trukketSøknadService: TrukketSøknadService,
    private val klageresultatUtleder: IKlageresultatUtleder,
    avbrytRevurderingService: AvbrytRevurderingService
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        sakService = SakService(repositoryProvider.provide(), repositoryProvider.provide()),
        tilkjentYtelseRepository = repositoryProvider.provide(),
        beregningsgrunnlagRepository = repositoryProvider.provide(),
        pipRepository = repositoryProvider.provide(),
        dokumentRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        meldekortRepository = repositoryProvider.provide(),
        påklagetBehandlingRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider.provide()),
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider)
    )

    private val log = LoggerFactory.getLogger(javaClass)

    private val resultatUtleder =
        ResultatUtleder(underveisRepository, behandlingRepository, trukketSøknadService, avbrytRevurderingService)

    fun oversettHendelseTilKontrakt(hendelse: BehandlingFlytStoppetHendelseTilStatistikk): StoppetBehandling {
        log.info("Oversetter hendelse for behandling ${hendelse.referanse} og saksnr ${hendelse.saksnummer}")
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val søknaderForSak = hentSøknaderForSak(behandling)
        val mottattTidspunkt = utledMottattTidspunkt(behandling, søknaderForSak)
        val søknadIder = søknaderForSak
            .filter { it.type == InnsendingType.SØKNAD }
            .map { it.referanse.asJournalpostId }

        val kanal = hentSøknadsKanal(behandling, søknaderForSak)

        val sak = sakService.hent(hendelse.saksnummer)

        val meldekort = meldekortRepository.hentHvisEksisterer(behandling.id)
        val forrigeBehandlingMeldekort =
            behandling.forrigeBehandlingId?.let { meldekortRepository.hentHvisEksisterer(it) }

        val nyeMeldekort =
            meldekort?.meldekort().orEmpty().toSet().minus(forrigeBehandlingMeldekort?.meldekort().orEmpty().toSet())
                .toList()

        val vurderingsbehovForBehandling = utledVurderingsbehovForBehandling(behandling)
        val statistikkHendelse = StoppetBehandling(
            saksnummer = hendelse.saksnummer.toString(),
            behandlingType = hendelse.behandlingType,
            behandlingStatus = hendelse.status,
            ident = hendelse.personIdent,
            avklaringsbehov = hendelse.avklaringsbehov,
            behandlingReferanse = hendelse.referanse.referanse,
            relatertBehandling = relatertBehandling(behandling),
            behandlingOpprettetTidspunkt = hendelse.opprettetTidspunkt,
            soknadsFormat = kanal,
            versjon = hendelse.versjon,
            mottattTid = mottattTidspunkt,
            sakStatus = sak.status(),
            hendelsesTidspunkt = hendelse.hendelsesTidspunkt,
            avsluttetBehandling = if (hendelse.status == AVSLUTTET) hentAvsluttetBehandlingDTO(hendelse) else null,
            identerForSak = hentIdenterPåSak(sak.saksnummer),
            vurderingsbehov = vurderingsbehovForBehandling,
            opprettetAv = hendelse.opprettetAv,
            nyeMeldekort = nyeMeldekort.map { meldekort ->
                MeldekortDTO(
                    meldekort.journalpostId.identifikator,
                    meldekort.timerArbeidPerPeriode.map {
                        ArbeidIPeriode(it.periode.fom, it.periode.tom, it.timerArbeid.antallTimer)
                    }
                )
            },
            søknadIder = søknadIder
        )
        return statistikkHendelse
    }

    private fun relatertBehandling(behandling: Behandling): UUID? {
        val påklagetBehandlingReferanse = if (behandling.typeBehandling() == TypeBehandling.Klage) {
            val påklagetBehandling =
                påklagetBehandlingRepository.hentGjeldendeVurderingMedReferanse(behandling.referanse)
            påklagetBehandling?.referanse
        } else null

        val forrigeBehandling =
            if (behandling.forrigeBehandlingId != null) behandlingRepository.hent(behandling.forrigeBehandlingId) else null

        if (forrigeBehandling != null && påklagetBehandlingReferanse != null) {
            log.error("Fant både forrigeBehandling og påklagetBehandlingReferanse for behandling ${behandling.referanse}. Returnerer forrigeBehandling.")
        }

        return forrigeBehandling?.referanse?.referanse
            ?: påklagetBehandlingReferanse?.referanse
    }

    private fun utledVurderingsbehovForBehandling(behandling: Behandling): List<no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov> =
        behandling.vurderingsbehov().map {
            when (it.type) {
                Vurderingsbehov.MOTTATT_SØKNAD -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SØKNAD
                Vurderingsbehov.MOTTATT_AKTIVITETSMELDING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.AKTIVITETSMELDING
                Vurderingsbehov.MOTTATT_MELDEKORT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.MELDEKORT
                Vurderingsbehov.MOTTATT_LEGEERKLÆRING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.LEGEERKLÆRING
                Vurderingsbehov.MOTTATT_AVVIST_LEGEERKLÆRING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.AVVIST_LEGEERKLÆRING
                Vurderingsbehov.MOTTATT_DIALOGMELDING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.DIALOGMELDING
                Vurderingsbehov.G_REGULERING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.G_REGULERING
                Vurderingsbehov.REVURDER_MEDLEMSKAP -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_MEDLEMSKAP
                Vurderingsbehov.REVURDER_BEREGNING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_BEREGNING
                Vurderingsbehov.REVURDER_YRKESSKADE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE
                Vurderingsbehov.REVURDER_LOVVALG -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_LOVVALG
                Vurderingsbehov.REVURDER_SAMORDNING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING
                Vurderingsbehov.REVURDER_STUDENT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_STUDENT
                Vurderingsbehov.MOTATT_KLAGE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.KLAGE
                Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP
                Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP
                Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
                Vurderingsbehov.BARNETILLEGG -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.BARNETILLEGG
                Vurderingsbehov.INSTITUSJONSOPPHOLD -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.INSTITUSJONSOPPHOLD
                Vurderingsbehov.SAMORDNING_OG_AVREGNING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SAMORDNING_OG_AVREGNING
                Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER
                Vurderingsbehov.REVURDER_SAMORDNING_UFØRE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING_UFØRE
                Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_STATLIGE_YTELSER -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_STATLIGE_YTELSER
                Vurderingsbehov.REVURDER_SAMORDNING_ARBEIDSGIVER -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING_ARBEIDSGIVER
                Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON
                Vurderingsbehov.REFUSJONSKRAV -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REFUSJONSKRAV
                Vurderingsbehov.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT
                Vurderingsbehov.FASTSATT_PERIODE_PASSERT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.MELDEKORT /* TODO: mer spesifikk? er pga fravær av meldekort */
                Vurderingsbehov.VURDER_RETTIGHETSPERIODE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.VURDER_RETTIGHETSPERIODE
                Vurderingsbehov.SØKNAD_TRUKKET -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SØKNAD_TRUKKET
                Vurderingsbehov.REVURDERING_AVBRUTT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDERING_AVBRUTT
                Vurderingsbehov.KLAGE_TRUKKET -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.KLAGE_TRUKKET
                Vurderingsbehov.REVURDER_MANUELL_INNTEKT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_MANUELL_INNTEKT
                Vurderingsbehov.FRITAK_MELDEPLIKT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.FRITAK_MELDEPLIKT
                Vurderingsbehov.MOTTATT_KABAL_HENDELSE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.MOTTATT_KABAL_HENDELSE
                Vurderingsbehov.HELHETLIG_VURDERING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.HELHETLIG_VURDERING
                Vurderingsbehov.OPPFØLGINGSOPPGAVE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OPPFØLGINGSOPPGAVE
                Vurderingsbehov.REVURDER_MELDEPLIKT_RIMELIG_GRUNN -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_MELDEPLIKT_RIMELIG_GRUNN
                Vurderingsbehov.AKTIVITETSPLIKT_11_7 -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.AKTIVITETSPLIKT_11_7
                Vurderingsbehov.AKTIVITETSPLIKT_11_9 -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.AKTIVITETSPLIKT_11_9
                Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT
                Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT_11_9
                Vurderingsbehov.OPPHOLDSKRAV -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OPPHOLDSKRAV
                Vurderingsbehov.OVERGANG_UFORE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OVERGANG_UFORE
                Vurderingsbehov.OVERGANG_ARBEID -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OVERGANG_ARBEID
                Vurderingsbehov.DØDSFALL_BRUKER -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.DØDSFALL_BRUKER
                Vurderingsbehov.DØDSFALL_BARN -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.DØDSFALL_BARN
            }
        }.distinct()

    private fun hentIdenterPåSak(saksnummer: Saksnummer): List<String> {
        return pipRepository.finnIdenterPåSak(saksnummer).map { it.ident }
    }

    private fun hentSøknadsKanal(behandling: Behandling, hentDokumenterAvType: Set<MottattDokument>): Kanal {
        val kanaler = hentDokumenterAvType.filter { it.behandlingId == behandling.id }.map { it.kanal }

        // Om minst én av søknadene er papir, regn med at hele behandlingen er papir
        return kanaler.reduceOrNull { acc, curr ->
            when (acc) {
                Kanal.DIGITAL -> curr
                Kanal.PAPIR -> Kanal.PAPIR
            }
        } ?: Kanal.DIGITAL
    }

    private fun utledMottattTidspunkt(
        behandling: Behandling, hentDokumenterAvType: Set<MottattDokument>
    ): LocalDateTime {
        val mottattTidspunkt =
            hentDokumenterAvType.filter { it.behandlingId == behandling.id }.minOfOrNull { it.mottattTidspunkt }

        if (mottattTidspunkt == null) {
            log.info("Ingen søknader funnet for behandling ${behandling.referanse} av type ${behandling.typeBehandling()}.")
            return behandling.opprettetTidspunkt
        }
        return mottattTidspunkt
    }

    private fun hentSøknaderForSak(behandling: Behandling): Set<MottattDokument> {
        val hentDokumenterAvType = dokumentRepository.hentDokumenterAvType(
            behandling.sakId, InnsendingType.SØKNAD
        )
        return hentDokumenterAvType
    }

    /**
     * Skal kalles når en behandling er avsluttet for å levere statistikk til statistikk-appen.
     * Payload er JSON siden dette kommer fra en jobb.
     */
    private fun hentAvsluttetBehandlingDTO(hendelse: BehandlingFlytStoppetHendelseTilStatistikk): AvsluttetBehandlingDTO {
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val vilkårsresultat = vilkårsresultatRepository.hent(behandling.id)
        val sak = sakService.hent(behandling.sakId)

        if (behandling.status() != AVSLUTTET) {
            log.warn("Kjører statistikkjobb for behandling som ikke er avsluttet. Behandling-ref: ${behandling.referanse.referanse}. Sak: ${sak.saksnummer}")
        }

        val tilkjentYtelse =
            tilkjentYtelseRepository.hentHvisEksisterer(behandling.id)?.map { Segment(it.periode, it.tilkjent) }
                ?.let(::Tidslinje)?.mapValue { it }?.komprimer()?.segmenter()?.map {
                    val verdi = it.verdi
                    TilkjentYtelsePeriodeDTO(
                        fraDato = it.periode.fom,
                        tilDato = it.periode.tom,
                        dagsats = verdi.dagsats.verdi().toDouble(),
                        gradering = verdi.gradering.endeligGradering.prosentverdi().toDouble(),
                        redusertDagsats = verdi.redusertDagsats().verdi().toDouble(),
                        antallBarn = verdi.antallBarn,
                        barnetilleggSats = verdi.barnetilleggsats.verdi().toDouble(),
                        barnetillegg = verdi.barnetillegg.verdi().toDouble(),
                    )
                }

        if (tilkjentYtelse == null) {
            log.info("Ingen tilkjente ytelser knyttet til avsluttet behandling ${behandling.id}.")
        }

        val grunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(behandling.id)

        val beregningsGrunnlagDTO: BeregningsgrunnlagDTO? =
            if (grunnlag == null) null else beregningsgrunnlagDTO(grunnlag)

        log.info("Kaller aap-statistikk for sak ${sak.saksnummer} og behandling ${behandling.referanse}")

        val rettighetstypePerioder = underveisRepository.hentHvisEksisterer(behandling.id)?.perioder.orEmpty()
            .filter { it.rettighetsType != null }.map { Segment(it.periode, it.rettighetsType) }.let(::Tidslinje)
            .komprimer().segmenter().map {
                RettighetstypePeriode(
                    fraDato = it.periode.fom,
                    tilDato = it.periode.tom,
                    rettighetstype = when (requireNotNull(it.verdi)) {
                        RettighetsType.BISTANDSBEHOV -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType.BISTANDSBEHOV
                        RettighetsType.SYKEPENGEERSTATNING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType.SYKEPENGEERSTATNING
                        RettighetsType.STUDENT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType.STUDENT
                        RettighetsType.ARBEIDSSØKER -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType.ARBEIDSSØKER
                        RettighetsType.VURDERES_FOR_UFØRETRYGD -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType.VURDERES_FOR_UFØRETRYGD
                    }
                )
            }

        val avsluttetBehandlingDTO = AvsluttetBehandlingDTO(
            vilkårsResultat = VilkårsResultatDTO(
                typeBehandling = behandling.typeBehandling(), vilkår = vilkårsresultat.alle().map { res ->
                    VilkårDTO(
                        vilkårType = Vilkårtype.valueOf(res.type.toString()),
                        perioder = res.vilkårsperioder().map { periode ->
                            VilkårsPeriodeDTO(
                                fraDato = periode.periode.fom,
                                tilDato = periode.periode.tom,
                                utfall = Utfall.valueOf(periode.utfall.toString()),
                                manuellVurdering = periode.manuellVurdering,
                                innvilgelsesårsak = periode.innvilgelsesårsak.toString(),
                                avslagsårsak = periode.avslagsårsak.toString()
                            )
                        })
                }),
            tilkjentYtelse = TilkjentYtelseDTO(perioder = tilkjentYtelse.orEmpty()),
            beregningsGrunnlag = beregningsGrunnlagDTO,
            diagnoser = hentDiagnose(behandling),
            rettighetstypePerioder = rettighetstypePerioder,
            resultat = hentResultat(behandling)
        )
        return avsluttetBehandlingDTO
    }

    private fun hentResultat(behandling: Behandling): ResultatKode? {
        return when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling -> {
                resultatUtleder.utledResultat(behandling.id).let {
                    when (it) {
                        Resultat.INNVILGELSE -> ResultatKode.INNVILGET
                        Resultat.AVSLAG -> ResultatKode.AVSLAG
                        Resultat.TRUKKET -> ResultatKode.TRUKKET
                        Resultat.AVBRUTT -> ResultatKode.AVBRUTT
                    }
                }
            }

            TypeBehandling.Klage -> {
                klageresultatUtleder.utledKlagebehandlingResultat(behandling.id).type.let {
                    when (it) {
                        KlageResultatType.OPPRETTHOLDES -> ResultatKode.KLAGE_OPPRETTHOLDES
                        KlageResultatType.OMGJØRES -> ResultatKode.KLAGE_OMGJØRES
                        KlageResultatType.DELVIS_OMGJØRES -> ResultatKode.KLAGE_DELVIS_OMGJØRES
                        KlageResultatType.AVSLÅTT -> ResultatKode.KLAGE_AVSLÅTT
                        KlageResultatType.TRUKKET -> ResultatKode.KLAGE_TRUKKET
                        KlageResultatType.UFULLSTENDIG -> null
                    }
                }
            }

            TypeBehandling.Revurdering -> {
                resultatUtleder.utledRevurderingResultat(behandling.id).let {
                    when (it) {
                        Resultat.AVBRUTT -> ResultatKode.AVBRUTT
                        else -> null
                    }
                }
            }

            TypeBehandling.Tilbakekreving,
            TypeBehandling.SvarFraAndreinstans,
            TypeBehandling.OppfølgingsBehandling,
            TypeBehandling.Aktivitetsplikt,
            TypeBehandling.Aktivitetsplikt11_9 -> {
                null
            }
        }
    }

    private fun hentDiagnose(behandling: Behandling): Diagnoser? {
        val sykdomsvurdering = sykdomRepository.hentHvisEksisterer(behandling.id)?.sykdomsvurderinger.orEmpty()
            .maxByOrNull { it.opprettet }

        if (sykdomsvurdering == null) {
            log.info("Fant ikke sykdomsvurdering for behandling ${behandling.referanse} (id: ${behandling.id})")
            return null
        }

        if (sykdomsvurdering.hoveddiagnose == null || sykdomsvurdering.kodeverk == null) {
            log.info("Fant sykdomsvurdering, men ingen diagnose eller kodeverk for behandling ${behandling.referanse} (id: ${behandling.id})")
            return null
        }

        return Diagnoser(
            kodeverk = sykdomsvurdering.kodeverk,
            diagnosekode = sykdomsvurdering.hoveddiagnose,
            bidiagnoser = sykdomsvurdering.bidiagnoser.orEmpty(),
        )
    }

    private fun beregningsgrunnlagDTO(
        grunnlag: Beregningsgrunnlag
    ): BeregningsgrunnlagDTO = when (grunnlag) {
        is Grunnlag11_19 -> BeregningsgrunnlagDTO(
            grunnlag11_19dto = grunnlag1119dto(grunnlag),
        )

        is GrunnlagUføre -> BeregningsgrunnlagDTO(
            grunnlagUføre = GrunnlagUføreDTO(
                grunnlaget = grunnlag.grunnlaget().verdi(),
                type = UføreType.valueOf(grunnlag.type().toString()),
                grunnlag = grunnlag1119dto(grunnlag.underliggende()),
                grunnlagYtterligereNedsatt = grunnlag1119dto(grunnlag.underliggendeYtterligereNedsatt()),
                uføreYtterligereNedsattArbeidsevneÅr = grunnlag.uføreYtterligereNedsattArbeidsevneÅr().value,
                uføregrad = grunnlag.uføregrad().prosentverdi(),
                uføreInntekterFraForegåendeÅr = grunnlag.uføreInntekterFraForegåendeÅr()
                    .associate { it.år.value.toString() to it.inntektIKroner.verdi().toDouble() })
        )

        is GrunnlagYrkesskade -> BeregningsgrunnlagDTO(
            grunnlagYrkesskade = GrunnlagYrkesskadeDTO(
                beregningsgrunnlag = when (grunnlag.underliggende()) {
                    is Grunnlag11_19 -> beregningsgrunnlagDTO(grunnlag.underliggende())
                    is GrunnlagUføre -> beregningsgrunnlagDTO(grunnlag.underliggende())
                    is GrunnlagYrkesskade -> throw RuntimeException("Grunnlagyrkesskade kan ikke ha grunnlag yrkesskade")
                },
                andelYrkesskade = grunnlag.andelYrkesskade().prosentverdi(),
                andelSomSkyldesYrkesskade = grunnlag.andelSomSkyldesYrkesskade().verdi(),
                andelSomIkkeSkyldesYrkesskade = grunnlag.andelSomIkkeSkyldesYrkesskade().verdi(),
                antattÅrligInntektYrkesskadeTidspunktet = grunnlag.antattÅrligInntektYrkesskadeTidspunktet().verdi(),
                benyttetAndelForYrkesskade = grunnlag.benyttetAndelForYrkesskade().prosentverdi(),
                grunnlagForBeregningAvYrkesskadeandel = grunnlag.grunnlagForBeregningAvYrkesskadeandel().verdi(),
                grunnlagEtterYrkesskadeFordel = grunnlag.grunnlagEtterYrkesskadeFordel().verdi(),
                terskelverdiForYrkesskade = grunnlag.terskelverdiForYrkesskade().prosentverdi(),
                yrkesskadeTidspunkt = grunnlag.yrkesskadeTidspunkt().value,
                yrkesskadeinntektIG = grunnlag.yrkesskadeinntektIG().verdi(),
                grunnlaget = grunnlag.grunnlaget().verdi(),
                inkludererUføre = grunnlag.underliggende() is GrunnlagUføre
            )
        )
    }

    private fun grunnlag1119dto(beregningsgrunnlag: Grunnlag11_19) = Grunnlag11_19DTO(
        inntekter = beregningsgrunnlag.inntekter()
            .associate { it.år.value.toString() to it.inntektIKroner.verdi().toDouble() },
        grunnlaget = beregningsgrunnlag.grunnlaget().verdi().toDouble(),
        er6GBegrenset = beregningsgrunnlag.inntekter().any { it.er6GBegrenset },
        erGjennomsnitt = beregningsgrunnlag.erGjennomsnitt(),
    )
}