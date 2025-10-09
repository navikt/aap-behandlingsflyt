package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Aktivitetskort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AktivitetskortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnnetRelevantDokumentV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Klage
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OmgjøringKlageRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Omgjøringskilde
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Oppfølgingsoppgave
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.tilVurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class HåndterMottattDokumentService(
    private val sakService: SakService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val mottaDokumentService: MottaDokumentService,
    private val behandlingRepository: BehandlingRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider),
        sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
        låsRepository = repositoryProvider.provide(),
        prosesserBehandling = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide<BehandlingRepository>(),

        )

    fun håndterMottatteKlage(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: Klage,
    ) {
        when (melding) {
            is KlageV0 -> {
                val sak = sakService.hent(sakId)
                val periode = utledPeriode(brevkategori, mottattTidspunkt, melding)
                val vurderingsbehov = utledVurderingsbehov(brevkategori, melding, periode)

                val behandling = if (melding.behandlingReferanse != null) {
                    behandlingRepository.hent(BehandlingReferanse(UUID.fromString(melding.behandlingReferanse)))
                } else {
                    sakOgBehandlingService.finnEllerOpprettBehandling(
                        sak.saksnummer,
                        VurderingsbehovOgÅrsak(
                            årsak = ÅrsakTilOpprettelse.KLAGE,
                            vurderingsbehov = vurderingsbehov,
                            beskrivelse = melding.beskrivelse,
                            opprettet = mottattTidspunkt
                        )
                    ).åpenBehandling
                }

                val behandlingSkrivelås = behandling?.let {
                    låsRepository.låsBehandling(it.id)
                }

                sakOgBehandlingService.oppdaterRettighetsperioden(sakId, brevkategori, mottattTidspunkt.toLocalDate())

                mottaDokumentService.markerSomBehandlet(sakId, behandling!!.id, referanse)

                prosesserBehandling.triggProsesserBehandling(
                    behandling,
                    listOf("trigger" to DefaultJsonMapper.toJson(vurderingsbehov.map { it.type }))
                )

                if (behandlingSkrivelås != null) {
                    låsRepository.verifiserSkrivelås(behandlingSkrivelås)
                }
            }
        }

    }

    fun håndterMottatteDokumenter(
        sakId: SakId,
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        brevkategori: InnsendingType,
        melding: Melding?,
    ) {
        log.info("Mottok dokument på sak-id $sakId, og referanse $referanse, med brevkategori $brevkategori.")
        val sak = sakService.hent(sakId)
        val periode = utledPeriode(brevkategori, mottattTidspunkt, melding)
        val vurderingsbehov = utledVurderingsbehov(brevkategori, melding, periode)
        val årsakTilOpprettelse = utledÅrsakTilOpprettelse(brevkategori, melding)

        val opprettetBehandling = sakOgBehandlingService.finnEllerOpprettBehandling(
            sak.saksnummer,
            VurderingsbehovOgÅrsak(
                årsak = årsakTilOpprettelse,
                vurderingsbehov = vurderingsbehov,
                opprettet = mottattTidspunkt,
                beskrivelse =  when (melding) {
                    is ManuellRevurderingV0 -> melding.beskrivelse
                    is OmgjøringKlageRevurderingV0 -> melding.beskrivelse
                    else -> null
                }
            )
        )

        val behandlingSkrivelås = opprettetBehandling.åpenBehandling?.let {
            låsRepository.låsBehandling(it.id)
        }

        sakOgBehandlingService.oppdaterRettighetsperioden(sakId, brevkategori, mottattTidspunkt.toLocalDate())

        if (skalMarkereDokumentSomBehandlet(melding)) {
            require(opprettetBehandling is SakOgBehandlingService.Ordinær)
            mottaDokumentService.markerSomBehandlet(sakId, opprettetBehandling.åpenBehandling.id, referanse)
        } else {
            when (opprettetBehandling) {
                is SakOgBehandlingService.Ordinær -> mottaDokumentService.oppdaterMedBehandlingId(sakId, opprettetBehandling.åpenBehandling.id, referanse)
                is SakOgBehandlingService.MåBehandlesAtomært -> mottaDokumentService.oppdaterMedBehandlingId(sakId, opprettetBehandling.nyBehandling.id, referanse)
            }
        }

        prosesserBehandling.triggProsesserBehandling(
            opprettetBehandling,
            listOf("trigger" to DefaultJsonMapper.toJson(vurderingsbehov.map { it.type }))
        )

        if (behandlingSkrivelås != null) {
            låsRepository.verifiserSkrivelås(behandlingSkrivelås)
        }
    }

    /**
     * Knytter klage og oppfølgingsbehandling direkte til behandlingen den opprettet, ikke via informasjonskrav.
     * Dette fordi det være flere åpne behandlinger av disse typene.
     * ManuellVurdering og NyÅrsakTilBehandling er knyttet eksplisitt til behandling og er ikke et informasjonskrav i flyten
     */
    private fun skalMarkereDokumentSomBehandlet(melding: Melding?): Boolean =
        melding is KabalHendelse || melding is Oppfølgingsoppgave || melding is ManuellRevurdering || melding is NyÅrsakTilBehandling

    fun oppdaterÅrsakerTilBehandlingPåEksisterendeÅpenBehandling(
        sakId: SakId,
        behandlingsreferanse: BehandlingReferanse,
        innsendingType: InnsendingType,
        melding: NyÅrsakTilBehandlingV0,
        referanse: InnsendingReferanse
    ) {
        val behandling = sakOgBehandlingService.finnBehandling(behandlingsreferanse)
        val årsakTilOpprettelse = utledÅrsakTilOpprettelse(innsendingType, melding)

        låsRepository.withLåstBehandling(behandling.id) {
            val vurderingsbehov =
                melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
            sakOgBehandlingService.oppdaterVurderingsbehovTilBehandling(behandling, VurderingsbehovOgÅrsak(vurderingsbehov, årsakTilOpprettelse))
            mottaDokumentService.markerSomBehandlet(sakId, behandling.id, referanse)
            prosesserBehandling.triggProsesserBehandling(
                sakId,
                behandling.id,
                listOf("trigger" to DefaultJsonMapper.toJson(vurderingsbehov.map { it.type }))
            )
        }
    }


    private fun utledÅrsakTilOpprettelse(brevkategori: InnsendingType, melding: Melding?): ÅrsakTilOpprettelse {
        return when (brevkategori) {
            InnsendingType.SØKNAD -> ÅrsakTilOpprettelse.SØKNAD
            InnsendingType.AKTIVITETSKORT -> ÅrsakTilOpprettelse.AKTIVITETSMELDING
            InnsendingType.MELDEKORT -> ÅrsakTilOpprettelse.MELDEKORT
            InnsendingType.LEGEERKLÆRING -> ÅrsakTilOpprettelse.HELSEOPPLYSNINGER
            InnsendingType.LEGEERKLÆRING_AVVIST -> ÅrsakTilOpprettelse.HELSEOPPLYSNINGER
            InnsendingType.DIALOGMELDING -> ÅrsakTilOpprettelse.HELSEOPPLYSNINGER
            InnsendingType.KLAGE -> ÅrsakTilOpprettelse.KLAGE
            InnsendingType.ANNET_RELEVANT_DOKUMENT -> ÅrsakTilOpprettelse.ANNET_RELEVANT_DOKUMENT
            InnsendingType.MANUELL_REVURDERING -> ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING -> ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            InnsendingType.KABAL_HENDELSE -> ÅrsakTilOpprettelse.SVAR_FRA_KLAGEINSTANS
            InnsendingType.OPPFØLGINGSOPPGAVE -> utledÅrsakTilOppfølgningsOppave(melding)
            InnsendingType.PDL_HENDELSE_DODSFALL_BRUKER -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
            InnsendingType.PDL_HENDELSE_DODSFALL_BARN -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
            InnsendingType.OMGJØRING_KLAGE_REVURDERING -> utledÅrsakEtterOmgjøringAvKlage(melding)
        }
    }
    private fun utledÅrsakTilOppfølgningsOppave(melding: Melding?): ÅrsakTilOpprettelse {
        require(melding is OppfølgingsoppgaveV0) { "Melding must be of type OppfølgingsoppgaveV0" }
        val kode = melding.opprinnelse?.avklaringsbehovKode
        val stegType = kode?.let { finnStegType(it) }
        return when (stegType) {
            StegType.SAMORDNING_GRADERING -> ÅrsakTilOpprettelse.OPPFØLGINGSOPPGAVE_SAMORDNING_GRADERING
            else -> ÅrsakTilOpprettelse.OPPFØLGINGSOPPGAVE
        }
    }


    private fun finnStegType(avklaringsTypeKode:String): StegType {
        return Definisjon.forKode(avklaringsTypeKode).løsesISteg
    }

    private fun utledÅrsakEtterOmgjøringAvKlage(melding: Melding?): ÅrsakTilOpprettelse = when (melding) {
        is OmgjøringKlageRevurderingV0 -> when (melding.kilde) {
            Omgjøringskilde.KLAGEINSTANS -> ÅrsakTilOpprettelse.OMGJØRING_ETTER_SVAR_FRA_KLAGEINSTANS
            Omgjøringskilde.KELVIN -> ÅrsakTilOpprettelse.OMGJØRING_ETTER_KLAGE
        }

        else -> error("Melding må være OmgjøringKlageRevurderingV0")
    }

    private fun utledVurderingsbehov(
        brevkategori: InnsendingType,
        melding: Melding?,
        periode: Periode?
    ): List<VurderingsbehovMedPeriode> {
        return when (brevkategori) {
            InnsendingType.SØKNAD -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD))
            InnsendingType.MANUELL_REVURDERING -> when (melding) {
                is ManuellRevurderingV0 -> melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                else -> error("Melding må være ManuellRevurderingV0")
            }

            InnsendingType.OMGJØRING_KLAGE_REVURDERING -> when (melding) {
                is OmgjøringKlageRevurderingV0 -> melding.vurderingsbehov.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                else -> error("Melding må være OmgjøringKlageRevurderingV0")
            }

            InnsendingType.MELDEKORT ->
                listOf(
                    VurderingsbehovMedPeriode(
                        type = Vurderingsbehov.MOTTATT_MELDEKORT,
                        periode = periode
                    )
                )

            InnsendingType.AKTIVITETSKORT -> listOf(
                VurderingsbehovMedPeriode(
                    type = Vurderingsbehov.MOTTATT_AKTIVITETSMELDING,
                    periode = periode
                )
            )

            InnsendingType.LEGEERKLÆRING_AVVIST -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_AVVIST_LEGEERKLÆRING))
            InnsendingType.LEGEERKLÆRING -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_LEGEERKLÆRING))
            InnsendingType.DIALOGMELDING -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_DIALOGMELDING))
            InnsendingType.ANNET_RELEVANT_DOKUMENT ->
                when (melding) {
                    is AnnetRelevantDokumentV0 -> melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                    else -> error("Melding må være AnnetRelevantDokumentV0")
                }

            InnsendingType.KLAGE -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTATT_KLAGE))
            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING ->
                when (melding) {
                    is NyÅrsakTilBehandlingV0 -> melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                    else -> error("Melding må være NyÅrsakTilBehandlingV0")
                }

            InnsendingType.KABAL_HENDELSE -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_KABAL_HENDELSE))
            InnsendingType.OPPFØLGINGSOPPGAVE -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.OPPFØLGINGSOPPGAVE))
            InnsendingType.PDL_HENDELSE_DODSFALL_BRUKER -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.DØDSFALL_BRUKER))
            InnsendingType.PDL_HENDELSE_DODSFALL_BARN -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.DØDSFALL_BARN))
        }
    }

    private fun utledPeriode(
        innsendingType: InnsendingType,
        mottattTidspunkt: LocalDateTime,
        melding: Melding?,
    ): Periode? {
        return when (innsendingType) {
            InnsendingType.SØKNAD -> Periode(
                mottattTidspunkt.toLocalDate(),
                mottattTidspunkt.plusYears(1).toLocalDate()
            )

            InnsendingType.AKTIVITETSKORT -> if (melding is Aktivitetskort) {
                when (melding) {
                    is AktivitetskortV0 -> Periode(fom = melding.fraOgMed, tom = melding.tilOgMed)
                }

            } else error("Må være aktivitetskort")

            InnsendingType.MELDEKORT -> if (melding is Meldekort) {
                when (melding) {
                    is MeldekortV0 -> Periode(
                        fom = melding.fom() ?: mottattTidspunkt.toLocalDate(),
                        tom = melding.tom() ?: mottattTidspunkt.toLocalDate()
                    )
                }
            } else error("Må være meldekort")

            else -> null
        }
    }
}
