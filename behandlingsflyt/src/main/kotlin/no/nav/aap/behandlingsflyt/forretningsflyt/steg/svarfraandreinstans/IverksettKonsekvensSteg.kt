package no.nav.aap.behandlingsflyt.forretningsflyt.steg.svarfraandreinstans

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansKonsekvens
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansVurdering
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
import java.util.UUID

class IverksettKonsekvensSteg private constructor(
    private val svarFraAndreinstansRepository: SvarFraAndreinstansRepository,
    private val flytJobbRepository: FlytJobbRepository,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vurdering = svarFraAndreinstansRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurdering

        requireNotNull(vurdering) {
            "IverksettKonsekvensSteg forventer at det finnes en vurdering av konsekvens"
        }

        when (vurdering.konsekvens) {
            SvarFraAndreinstansKonsekvens.INGENTING -> {}
            SvarFraAndreinstansKonsekvens.OMGJØRING -> opprettRevurdering(kontekst, vurdering)
            SvarFraAndreinstansKonsekvens.BEHANDLE_PÅ_NYTT -> opprettNyKlagebehandling()
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return IverksettKonsekvensSteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.IVERKSETT_KONSEKVENS
        }
    }

    private fun opprettRevurdering(kontekst: FlytKontekstMedPerioder, vurdering: SvarFraAndreinstansVurdering) {
        flytJobbRepository.leggTil(
            HendelseMottattHåndteringJobbUtfører.nyJobb(
                sakId = kontekst.sakId,
                dokumentReferanse = InnsendingReferanse(
                    InnsendingId(UUID.randomUUID())
                ),
                brevkategori = InnsendingType.MANUELL_REVURDERING,
                kanal = Kanal.DIGITAL,
                melding = konstruerMelding(vurdering),
                mottattTidspunkt = LocalDateTime.now()
            ),
        )
    }
    
    private fun opprettNyKlagebehandling() {
        TODO("Ikke implementert enda")
    }

    private fun konstruerMelding(vurdering: SvarFraAndreinstansVurdering): ManuellRevurdering {
        require(vurdering.vilkårSomOmgjøres.isNotEmpty()) {
            "For å opprette en ManuellRevurdering må det være minst ett vilkår som skal omgjøres"
        }
        val hjemler = vurdering.vilkårSomOmgjøres
        val beskrivelse = konstruerBegrunnelse(hjemler)
        val årsaker = hjemler.map { it.tilÅrsak() }

        return ManuellRevurderingV0(
            årsakerTilBehandling = årsaker,
            beskrivelse = beskrivelse,
        )
    }

    private fun konstruerBegrunnelse(hjemler: List<Hjemmel>): String {
        return "Revurdering etter klage som er tatt til følge i Nav Klageinstans. Følgende vilkår omgjøres: ${
            hjemler.joinToString(", ") { it.hjemmel }
        }"
    }
}