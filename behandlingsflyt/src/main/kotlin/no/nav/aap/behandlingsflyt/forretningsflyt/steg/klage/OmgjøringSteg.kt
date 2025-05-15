package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.KlageResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Omgjøres
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime
import java.util.*

class OmgjøringSteg private constructor(
    private val klageresultatUtleder: KlageresultatUtleder,
    private val flytJobbRepository: FlytJobbRepository,
    private val trekkKlageService: TrekkKlageService,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if(trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            return Fullført
        }

        val resultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)

        // / TODO: Opprett egen innsendingstype for klageomgjøring

        return when (resultat) {
            is Omgjøres, is DelvisOmgjøres -> {
                flytJobbRepository.leggTil(
                    HendelseMottattHåndteringJobbUtfører.nyJobb(
                        sakId = kontekst.sakId,
                        dokumentReferanse = InnsendingReferanse(
                            InnsendingId(UUID.randomUUID())
                        ),
                        brevkategori = InnsendingType.MANUELL_REVURDERING,
                        kanal = Kanal.DIGITAL,
                        melding = konstruerMelding(resultat),
                        mottattTidspunkt = LocalDateTime.now()
                    ),
                )
                Fullført
            }

            else -> Fullført
        }
    }

    private fun konstruerMelding(klageresultat: KlageResultat): ManuellRevurdering {
        require(klageresultat is DelvisOmgjøres || klageresultat is Omgjøres) {
            "Klagebehandlingresultat skal være Omgjøres eller DelvisOmgjøres"
        }
        val hjemler = klageresultat.hjemlerSomSkalOmgjøres()
        val beskrivelse = konstruerBegrunnelse(hjemler)
        val årsaker = hjemler.map { it.tilÅrsak() }

        return ManuellRevurderingV0(
            årsakerTilBehandling = årsaker,
            beskrivelse = beskrivelse,
        )
    }

    private fun konstruerBegrunnelse(hjemler: List<Hjemmel>): String {
        return "Revurdering etter klage som tas til følge. Følgende vilkår omgjøres: ${
            hjemler.map { it.hjemmel }.joinToString(", ")
        }"
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return OmgjøringSteg(
                KlageresultatUtleder(repositoryProvider),
                repositoryProvider.provide(),
                trekkKlageService = TrekkKlageService(repositoryProvider),
            )
        }

        override fun type(): StegType {
            return StegType.OMGJØRING
        }
    }
}