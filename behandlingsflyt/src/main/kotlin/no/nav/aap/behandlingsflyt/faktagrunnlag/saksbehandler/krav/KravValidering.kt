package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import java.time.DayOfWeek
import java.time.LocalDate

object KravValidering {
    fun erKravVurderingTilstrekkeligVurdert(søknaderIBehandling: Set<MottattDokument>, kravVurderinger: Set<KravVurdering>): Boolean {
        val erAlleSøknaderIBehandlingVurdert =
            søknaderIBehandling.all { søknad -> kravVurderinger.any { it.forJournalpostId(søknad.referanse.asJournalpostId) } }

        return erAlleSøknaderIBehandlingVurdert
    }

    fun validerRelevantKrav(
        vurdering: RelevantKrav,
        søknadForVurdering: MottattDokument?
    ) {
        if (søknadForVurdering != null) {
            if (vurdering.søknadsdato.årsak == SøknadsdatoÅrsak.SøknadMottatt && søknadForVurdering.mottattTidspunkt.toLocalDate() != vurdering.søknadsdato.dato) {
                throw UgyldigForespørselException("Søknadsdato for krav må være lik mottatt dato for den digitaliserte søknaden.")
            }
        }
        val overstyrMuligRettFra = vurdering.overstyrMuligRettFra
        if (overstyrMuligRettFra != null && overstyrMuligRettFra.dato > vurdering.søknadsdato.dato) {
            throw UgyldigForespørselException("Med rett fra annen dato enn søknadsdato må den nye rettighetsdatoen være tidligere enn søknadsdatoen.")
        }
    }

    fun validerMigrertKrav(vurdering: MigrertKrav) {
        if (vurdering.muligRettFra.isAfter(LocalDate.now())) {
            throw UgyldigForespørselException("migreringstidspunkt kan ikke være satt frem i tid.")
        }

        if (vurdering.muligRettFra.dayOfWeek != DayOfWeek.MONDAY) {
            throw UgyldigForespørselException("migreringstidspunkt for migrert krav må være en mandag.")
        }

        if (!vurdering.muligRettFra.isAfter(vurdering.virkningstidspunktArena)) {
            throw UgyldigForespørselException("migreringstidspunkt for migrert krav må være etter virkningstidspunktArena.")
        }

        val maksKvote = KvoteService.standardKvoter.ordinærkvote.asInt
        if (vurdering.resterendeKvoteOrdinaer <= 0 || vurdering.resterendeKvoteOrdinaer > maksKvote) {
            throw UgyldigForespørselException("resterende kvote for migrert krav må være høyere enn 0 og maks $maksKvote.")
        }

        // TODO: Midlertidig fordi vi kun støtter å migrere 1 type kvote enn så lenge
        if(vurdering.rettighetstype != MigrertRettighetstype.ORDINÆR) {
            throw UgyldigForespørselException("Kelvin støtter kun å migrere rettighetstype ORDINÆR.")
        }
    }
}