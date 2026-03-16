package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnnetRelevantDokument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OmgjøringKlageRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Omgjøringskilde
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.tilVurderingsbehov

object MottattHendelseUtleder {

    fun utledÅrsakTilOpprettelse(brevkategori: InnsendingType, melding: Melding?): ÅrsakTilOpprettelse {
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
            InnsendingType.TILBAKEKREVING_HENDELSE -> ÅrsakTilOpprettelse.TILBAKEKREVING_HENDELSE
            InnsendingType.INSTITUSJONSOPPHOLD -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
            InnsendingType.FAGSYSTEMINFO_BEHOV_HENDELSE -> ÅrsakTilOpprettelse.FAGSYSTEMINFO_BEHOV_HENDELSE
            InnsendingType.SYKEPENGE_VEDTAK_HENDELSE -> throw IllegalArgumentException("Sykepengevedtakhendelser skal trigge sjekk av informasjonskrav og ikke opprette en behandling direkte")
            InnsendingType.UFØRE_VEDTAK_HENDELSE -> ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
        }
    }

    fun utledVurderingsbehov(
        brevkategori: InnsendingType,
        melding: Melding?
    ): List<VurderingsbehovMedPeriode> {
        return when (brevkategori) {
            InnsendingType.SØKNAD -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD))
            InnsendingType.MANUELL_REVURDERING -> when (melding) {
                is ManuellRevurderingV0 -> melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                else -> error("Melding må være ManuellRevurderingV0")
            }

            InnsendingType.OMGJØRING_KLAGE_REVURDERING -> when (melding) {
                is OmgjøringKlageRevurdering -> melding.vurderingsbehov.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                else -> error("Melding må være OmgjøringKlageRevurderingV0")
            }

            InnsendingType.MELDEKORT ->
                listOf(
                    VurderingsbehovMedPeriode(
                        type = Vurderingsbehov.MOTTATT_MELDEKORT,
                    )
                )

            InnsendingType.AKTIVITETSKORT -> listOf(
                VurderingsbehovMedPeriode(
                    type = Vurderingsbehov.MOTTATT_AKTIVITETSMELDING,
                )
            )

            InnsendingType.LEGEERKLÆRING_AVVIST -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_AVVIST_LEGEERKLÆRING))
            InnsendingType.LEGEERKLÆRING -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_LEGEERKLÆRING))
            InnsendingType.DIALOGMELDING -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_DIALOGMELDING))
            InnsendingType.ANNET_RELEVANT_DOKUMENT ->
                when (melding) {
                    is AnnetRelevantDokument -> melding.årsakerTilBehandling.map { VurderingsbehovMedPeriode(it.tilVurderingsbehov()) }
                    else -> error("Melding må være AnnetRelevantDokument")
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
            InnsendingType.INSTITUSJONSOPPHOLD -> listOf(VurderingsbehovMedPeriode(Vurderingsbehov.INSTITUSJONSOPPHOLD))

            InnsendingType.TILBAKEKREVING_HENDELSE,
            InnsendingType.FAGSYSTEMINFO_BEHOV_HENDELSE,
            InnsendingType.SYKEPENGE_VEDTAK_HENDELSE,
            InnsendingType.UFØRE_VEDTAK_HENDELSE -> emptyList()
        }
    }

    fun utledBeskrivelseForÅrsakTilOpprettelse(melding: Melding?): String? = when (melding) {
        is ManuellRevurderingV0 -> melding.beskrivelse
        is OmgjøringKlageRevurdering -> melding.beskrivelse
        is PdlHendelseV0 -> melding.beskrivelse
        is NyÅrsakTilBehandlingV0 -> melding.årsakerTilBehandling.joinToString(", ")
        is AnnetRelevantDokument -> melding.begrunnelse
        else -> null
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

    private fun finnStegType(avklaringsTypeKode: String): StegType {
        return Definisjon.forKode(avklaringsTypeKode).løsesISteg
    }

    private fun utledÅrsakEtterOmgjøringAvKlage(melding: Melding?): ÅrsakTilOpprettelse = when (melding) {
        is OmgjøringKlageRevurdering -> when (melding.kilde) {
            Omgjøringskilde.KLAGEINSTANS -> ÅrsakTilOpprettelse.OMGJØRING_ETTER_SVAR_FRA_KLAGEINSTANS
            Omgjøringskilde.KELVIN -> ÅrsakTilOpprettelse.OMGJØRING_ETTER_KLAGE
        }

        else -> error("Melding må være OmgjøringKlageRevurderingV0")
    }

}