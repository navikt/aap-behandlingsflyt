package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Tilleggsopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate

private val log = LoggerFactory.getLogger(BackfillKravService::class.java)

class BackfillKravService(
    private val kravRepository: KravRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val rettighetsperiodeRepository: VurderRettighetsperiodeRepository,
    private val trukketSøknadService: TrukketSøknadService,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        kravRepository = repositoryProvider.provide(),
        stønadsperiodeRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
        rettighetsperiodeRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
    )

    fun erTrukketSøknadSak(behandlinger: List<Behandling>): Boolean =
        behandlinger.filter { it.status().erAvsluttet() }.any { trukketSøknadService.søknadErTrukket(it.id) }

    /**
     * Backfiller krav og stønadsperiode for én behandling.
     *
     * Returnerer [BackfillBehandlingResultat.AlleredeBackfilled] dersom behandlingen allerede hadde krav –
     * løkken i runner skal da bryte ut av saken, da resten er nyere og allerede har sine vurderinger.
     */
    fun backfillBehandling(sak: Sak, behandling: Behandling): BackfillBehandlingResultat {
        val eksisterendeKrav = kravRepository.hentHvisEksisterer(behandling.id)
        if (eksisterendeKrav != null) {
            return BackfillBehandlingResultat.AlleredeBackfilled
        }

        val søknader = mottattDokumentRepository
            .hentDokumenterAvType(behandling.id, InnsendingType.SØKNAD)
            .sortedBy { it.mottattTidspunkt }

        val forrigeKrav = behandling.forrigeBehandlingId?.let { kravRepository.hentHvisEksisterer(it) }

        val nyeVurderinger: Set<KravVurdering> = utledNyeVurderinger(behandling.id, søknader, forrigeKrav)

        val alleVurderinger = (forrigeKrav?.vurderinger.orEmpty()) + nyeVurderinger
        if (alleVurderinger.isEmpty()) {
            log.info("Ingen kravvurderinger for behandling ${behandling.id.toLong()} – hopper over")
            return BackfillBehandlingResultat.Backfilled
        }

        val grunnlag = KravGrunnlag(alleVurderinger.toSet())
        val oppdatertGrunnlag = håndterRettighetsperiodevurdering(behandling.id, grunnlag)

        verifiserMotRettighetsperiode(sak, oppdatertGrunnlag)

        kravRepository.lagre(behandling.id, oppdatertGrunnlag.vurderinger)
        backfillStønadsperiode(behandling.id, oppdatertGrunnlag)

        return BackfillBehandlingResultat.Backfilled
    }

    private fun utledNyeVurderinger(
        behandlingId: BehandlingId,
        søknader: List<MottattDokument>,
        forrigeKrav: KravGrunnlag?,
    ): Set<KravVurdering> {
        if (søknader.isEmpty()) return emptySet()

        val harForrigeRelevantKrav = forrigeKrav?.gjeldendeRelevanteKrav()?.isNotEmpty() == true

        return søknader.mapIndexed { index, søknad ->
            if (!harForrigeRelevantKrav && index == 0) {
                RelevantKrav(
                    referanse = Kravreferanse.ny(),
                    journalpostId = søknad.referanse.asJournalpostId,
                    vurdertAv = SYSTEMBRUKER,
                    begrunnelse = "Automatisk vurdering",
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(),
                    søknadsdato = Søknadsdato(søknad.mottattTidspunkt.toLocalDate(), SøknadsdatoÅrsak.SøknadMottatt),
                    overstyrMuligRettFra = null,
                    muligRettFra = søknad.mottattTidspunkt.toLocalDate(),
                )
            } else {
                Tilleggsopplysning(
                    referanse = Kravreferanse.ny(),
                    journalpostId = søknad.referanse.asJournalpostId,
                    vurdertAv = SYSTEMBRUKER,
                    begrunnelse = "Automatisk vurdering",
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(),
                )
            }
        }.toSet()
    }

    /**
     * Dersom det finnes en vedtatt rettighetsperiodevurdering med overstyring, oppdateres relevante krav:
     * - [OverstyrMuligRettFra] settes med dato og årsak
     * - [RelevantKrav.muligRettFra] settes til det tidligste av mottattdato og overstyrt dato
     */
    private fun håndterRettighetsperiodevurdering(
        behandlingId: BehandlingId,
        grunnlag: KravGrunnlag,
    ): KravGrunnlag {
        val vurdering = rettighetsperiodeRepository.hentVurdering(behandlingId) ?: return grunnlag
        if (!vurdering.harRettUtoverSøknadsdato.harOverstyrt() || vurdering.startDato == null) return grunnlag

        val oppdaterteVurderinger = grunnlag.vurderinger.map { krav ->
            if (krav !is RelevantKrav) return@map krav
            val gjeldendeMuligRettFra = minOf(krav.muligRettFra, vurdering.startDato)
            krav.copy(
                overstyrMuligRettFra = OverstyrMuligRettFra(
                    dato = vurdering.startDato,
                    årsak = vurdering.harRettUtoverSøknadsdato.tilOverstyrMuligRettFraÅrsak(),
                ),
                muligRettFra = gjeldendeMuligRettFra,
            )
        }.toSet()

        return grunnlag.copy(vurderinger = oppdaterteVurderinger)
    }

    /**
     * Verifiserer at saken sin rettighetsperiode.fom stemmer med gjeldende krav sin muligRettFra.
     * Kræsjer dersom de ikke er like – dette indikerer en datakonsistensfeil.
     */
    private fun verifiserMotRettighetsperiode(sak: Sak, grunnlag: KravGrunnlag) {
        val gjeldendeKrav = grunnlag.gjeldendeRelevanteKrav()
        if (gjeldendeKrav.isEmpty()) return

        val gjeldendeMuligRettFra: LocalDate = gjeldendeKrav.minOf { it.muligRettFra }
        check(sak.rettighetsperiode.fom == gjeldendeMuligRettFra) {
            "rettighetsperiode.fom (${sak.rettighetsperiode.fom}) stemmer ikke med gjeldende krav muligRettFra " +
                    "($gjeldendeMuligRettFra) for sak ${sak.id.toLong()}"
        }
    }

    private fun backfillStønadsperiode(behandlingId: BehandlingId, grunnlag: KravGrunnlag) {
        val gjeldendeRelevanteKrav = grunnlag.gjeldendeRelevanteKrav()
        if (gjeldendeRelevanteKrav.isEmpty()) return

        val referanser = gjeldendeRelevanteKrav.map { it.referanse }
        check(referanser.size <= 1) {
            "Fant flere distinkte kravreferanser blant relevante krav for behandling $behandlingId – forventet maks én"
        }

        if (stønadsperiodeRepository.hentHvisEksisterer(behandlingId) != null) return

        val nyeVurderinger = gjeldendeRelevanteKrav.map { krav ->
            StønadsperiodeVurdering(
                referanse = krav.referanse,
                opprettet = Instant.now(),
                vurdertIBehandling = behandlingId,
                vurdertAv = SYSTEMBRUKER,
                begrunnelse = "Automatisk vurdert",
                harHattOrdinærSiste52Uker = false,
                harGjenværendeKvote = false,
                relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
                startDato = krav.muligRettFra,
            )
        }.toSet()

        stønadsperiodeRepository.lagre(behandlingId, nyeVurderinger)
    }
}

enum class BackfillBehandlingResultat {
    /** Behandlingen hadde allerede krav. Resten av saken kan hoppes over. */
    AlleredeBackfilled,

    /** Krav ble backfilled (eller det var ingenting å gjøre). */
    Backfilled,
}
