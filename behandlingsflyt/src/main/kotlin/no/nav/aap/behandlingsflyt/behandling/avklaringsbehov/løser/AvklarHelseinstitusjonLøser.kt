package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AvklarHelseinstitusjonLøser(
    private val helseinstitusjonRepository: InstitusjonsoppholdRepository,
    private val behandlingRepository: BehandlingRepository
) : AvklaringsbehovsLøser<AvklarHelseinstitusjonLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        helseinstitusjonRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarHelseinstitusjonLøsning
    ): LøsningsResultat {
        val grunnlag = helseinstitusjonRepository.hentHvisEksisterer(kontekst.kontekst.behandlingId)
        val oppholdSegmenter = grunnlag?.oppholdene?.opphold ?: emptyList()
        val behandling = behandlingRepository.hent(kontekst.behandlingId())
        val vurdertAv = kontekst.bruker.ident

        validerReduksjonsdatoForInstitusjonsopphold(
            oppholdSegmenter,
            løsning.helseinstitusjonVurdering.vurderinger
        ).throwOnInvalid() {
            UgyldigForespørselException(it.errorMessage)
        }

        val vedtatteVurderinger =
            behandling.forrigeBehandlingId?.let { helseinstitusjonRepository.hentHvisEksisterer(it) }

        val oppdaterteVurderinger =
            slåSammenMedNyeVurderinger(
                vedtatteVurderinger,
                løsning.helseinstitusjonVurdering.vurderinger,
                behandling,
                vurdertAv
            )

        helseinstitusjonRepository.lagreHelseVurdering(
            kontekst.kontekst.behandlingId,
            vurdertAv,
            oppdaterteVurderinger
        )

        return LøsningsResultat(løsning.helseinstitusjonVurdering.vurderinger.joinToString(" ") { it.begrunnelse })
    }

    private fun slåSammenMedNyeVurderinger(
        grunnlag: InstitusjonsoppholdGrunnlag?,
        nyeVurderinger: List<HelseinstitusjonVurderingDto>,
        behandling: Behandling,
        vurdertAv: String,
    ): List<HelseinstitusjonVurdering> {
        val eksisterendeTidslinje = byggTidslinjeForHelseoppholdvurderinger(grunnlag)

        val nyeVurderingerTidslinje = Tidslinje(nyeVurderinger.sortedBy { it.periode }
            .map {
                Segment(
                    it.periode,
                    HelseoppholdVurderingData(
                        begrunnelse = it.begrunnelse,
                        faarFriKostOgLosji = it.faarFriKostOgLosji,
                        forsoergerEktefelle = it.forsoergerEktefelle,
                        harFasteUtgifter = it.harFasteUtgifter,
                        vurdertIBehandling = behandling.id,
                        vurdertAv = vurdertAv,
                        vurdertTidspunkt = LocalDateTime.now()
                    )
                )
            }).komprimer()

        return eksisterendeTidslinje.kombiner(
            nyeVurderingerTidslinje,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        ).segmenter().map {
            HelseinstitusjonVurdering(
                begrunnelse = it.verdi.begrunnelse,
                faarFriKostOgLosji = it.verdi.faarFriKostOgLosji,
                forsoergerEktefelle = it.verdi.forsoergerEktefelle,
                harFasteUtgifter = it.verdi.harFasteUtgifter,
                periode = it.periode,
                vurdertIBehandling = it.verdi.vurdertIBehandling,
                vurdertAv = vurdertAv,
                vurdertTidspunkt = it.verdi.vurdertTidspunkt
            )
        }
    }

    private fun byggTidslinjeForHelseoppholdvurderinger(grunnlag: InstitusjonsoppholdGrunnlag?): Tidslinje<HelseoppholdVurderingData> {
        if (grunnlag == null) {
            return Tidslinje()
        }
        return grunnlag.helseoppholdvurderinger?.tilTidslinje()
            ?.mapValue {
                HelseoppholdVurderingData(
                    begrunnelse = it.begrunnelse,
                    faarFriKostOgLosji = it.faarFriKostOgLosji,
                    forsoergerEktefelle = it.forsoergerEktefelle,
                    harFasteUtgifter = it.harFasteUtgifter,
                    vurdertIBehandling = it.vurdertIBehandling,
                    vurdertAv = it.vurdertAv,
                    vurdertTidspunkt = it.vurdertTidspunkt
                )
            }.orEmpty()
    }

    private fun validerReduksjonsdatoForInstitusjonsopphold(
        opphold: List<Segment<Institusjon>>,
        vurderinger: List<HelseinstitusjonVurderingDto>
    ): Validation<List<HelseinstitusjonVurderingDto>> {
        if (opphold.isEmpty() || vurderinger.isEmpty()) return Validation.Valid(vurderinger)

        // Finn vurderinger per opphold ved å bruke overlapp
        val vurderingerPerOpphold = opphold.associateWith { o ->
            vurderinger
                .filter { v -> v.periode.fom >= o.periode.fom && v.periode.tom <= o.periode.tom }
                .sortedBy { it.periode }
        }

        // Valider første opphold: Ingen reduksjon første 4 måneder (innleggelsesmåned + 3 måneder)
        vurderingerPerOpphold.entries.firstOrNull()?.let { (opphold, vurderinger) ->
            val første = førsteReduksjonsvurdering(vurderinger)
            val tidligsteReduksjonsdato = opphold.periode.fom.withDayOfMonth(1).plusMonths(4)
            val resultat = validerReduksjonsdato(
                vurderinger, første, tidligsteReduksjonsdato,
                "Første reduksjonsvurdering starter for tidlig. Skal ikke starte før"
            )

            if (resultat != null) return resultat
        }

        // Valider påfølgende opphold: Hvis nytt opphold innen 3 måneder etter utskrivelse
        if (vurderingerPerOpphold.size > 1) {
            val oppholdsliste = vurderingerPerOpphold.entries.toList()

            for (i in 1 until oppholdsliste.size) {
                val forrigeOpphold = oppholdsliste[i - 1].key
                val nåværendeOpphold = oppholdsliste[i].key
                val vurderingerNåværende = oppholdsliste[i].value

                val treMånederEtterUtskrivelse = forrigeOpphold.periode.tom.plusMonths(3)
                val erInnenTreMåneder = !nåværendeOpphold.periode.fom.isAfter(treMånederEtterUtskrivelse)
                val første = førsteReduksjonsvurdering(vurderingerNåværende)

                if (erInnenTreMåneder) {
                    // Reduksjon skal starte fra måneden etter nytt opphold
                    val tidligsteReduksjonsdato = nåværendeOpphold.periode.fom.withDayOfMonth(1).plusMonths(1)
                    val resultat = validerReduksjonsdato(
                        vurderingerNåværende, første, tidligsteReduksjonsdato,
                        "Reduksjon ved nytt opphold innen 3 måneder starter for tidlig. Skal starte fra"
                    )

                    if (resultat != null) return resultat
                } else {
                    // Nytt opphold behandles som første opphold (4 måneders karantene)
                    val tidligsteReduksjonsdato = nåværendeOpphold.periode.fom.withDayOfMonth(1).plusMonths(4)
                    val resultat = validerReduksjonsdato(
                        vurderinger, første, tidligsteReduksjonsdato,
                        "Reduksjon ved nytt opphold starter for tidlig. Skal ikke starte før"
                    )

                    if (resultat != null) return resultat
                }
            }
        }
        return Validation.Valid(vurderinger)
    }

    private fun førsteReduksjonsvurdering(vurderinger: List<HelseinstitusjonVurderingDto>): HelseinstitusjonVurderingDto? {
        return vurderinger.firstOrNull {
            it.faarFriKostOgLosji && it.forsoergerEktefelle == false && it.harFasteUtgifter == false
        }
    }

    private fun validerReduksjonsdato(
        vurderinger: List<HelseinstitusjonVurderingDto>,
        førsteReduksjonsvurdering: HelseinstitusjonVurderingDto?,
        tidligsteReduksjonsdato: LocalDate,
        feilmelding: String
    ): Validation<List<HelseinstitusjonVurderingDto>>? {
        if (førsteReduksjonsvurdering != null && førsteReduksjonsvurdering.periode.fom.isBefore(tidligsteReduksjonsdato)) {
            val tidligsteReduksjonsdatoFormatert =
                tidligsteReduksjonsdato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            return Validation.Invalid(
                vurderinger,
                "$feilmelding $tidligsteReduksjonsdatoFormatert"
            )
        }
        return null
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_HELSEINSTITUSJON
    }

    internal data class HelseoppholdVurderingData(
        val begrunnelse: String,
        val faarFriKostOgLosji: Boolean,
        val forsoergerEktefelle: Boolean? = null,
        val harFasteUtgifter: Boolean? = null,
        val vurdertIBehandling: BehandlingId,
        val vurdertAv: String,
        val vurdertTidspunkt: LocalDateTime
    )
}