package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.SammenhengendeOppholdGruppe
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.beregnTidligsteReduksjonsdatoPerOpphold
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.grupperSammenhengendeOppholdSegmenter
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
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AvklarHelseinstitusjonLøser(
    private val behandlingRepository: BehandlingRepository,
    private val helseinstitusjonRepository: InstitusjonsoppholdRepository
) : AvklaringsbehovsLøser<AvklarHelseinstitusjonLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        helseinstitusjonRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarHelseinstitusjonLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())
        val vurdertAv = kontekst.bruker

        validerReduksjonsdatoForInstitusjonsopphold(
            behandling,
            løsning.helseinstitusjonVurdering.vurderinger
        ).throwOnInvalid {
            UgyldigForespørselException(it.errorMessage)
        }

        val oppdaterteVurderinger =
            slåSammenMedNyeVurderinger(
                løsning.helseinstitusjonVurdering.vurderinger,
                behandling,
                vurdertAv
            )

        helseinstitusjonRepository.lagreHelseVurdering(
            kontekst.kontekst.behandlingId,
            oppdaterteVurderinger
        )

        return LøsningsResultat(løsning.helseinstitusjonVurdering.vurderinger.joinToString(" ") { it.begrunnelse })
    }

    private fun slåSammenMedNyeVurderinger(
        nyeVurderinger: List<HelseinstitusjonVurderingDto>,
        behandling: Behandling,
        vurdertAv: Bruker,
    ): List<HelseinstitusjonVurdering> {
        val forrigeGrunnlag =
            behandling.forrigeBehandlingId?.let { helseinstitusjonRepository.hentHvisEksisterer(it) }

        val eksisterendeTidslinje =
            byggTidslinjeForHelseoppholdvurderingerBegrensetTilOpphold(forrigeGrunnlag, behandling.id)

        if (nyeVurderinger.isEmpty()) {
            return eksisterendeTidslinje.segmenter().map {
                HelseinstitusjonVurdering(
                    begrunnelse = it.verdi.begrunnelse,
                    faarFriKostOgLosji = it.verdi.faarFriKostOgLosji,
                    forsoergerEktefelle = it.verdi.forsoergerEktefelle,
                    harFasteUtgifter = it.verdi.harFasteUtgifter,
                    periode = it.periode,
                    vurdertIBehandling = it.verdi.vurdertIBehandling,
                    vurdertAv = it.verdi.vurdertAv,
                    vurdertTidspunkt = it.verdi.vurdertTidspunkt
                )
            }
        }

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
        ).komprimer().segmenter().map {
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

    private fun byggTidslinjeForHelseoppholdvurderingerBegrensetTilOpphold(
        forrigeGrunnlag: InstitusjonsoppholdGrunnlag?,
        nåværendeBehandlingId: BehandlingId
    ): Tidslinje<HelseoppholdVurderingData> {
        val nåværendeGrunnlag = helseinstitusjonRepository.hentHvisEksisterer(nåværendeBehandlingId)
        val nåværendeOppholdTom = nåværendeGrunnlag?.oppholdene?.opphold?.maxOfOrNull { it.periode.tom }
        val tidslinje = forrigeGrunnlag?.helseoppholdvurderinger?.tilTidslinje()
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

        if (nåværendeOppholdTom == null) return tidslinje

        val begrensetSegmenter = tidslinje.segmenter()
            .filter { it.periode.fom <= nåværendeOppholdTom }
            .map { segment ->
                if (segment.periode.tom > nåværendeOppholdTom)
                    Segment(
                        Periode(segment.periode.fom, nåværendeOppholdTom),
                        segment.verdi.copy(
                            vurdertTidspunkt = LocalDateTime.now()
                        )
                    )
                else
                    segment
            }

        return if (begrensetSegmenter.isEmpty()) Tidslinje(emptyList()) else Tidslinje(begrensetSegmenter)
    }

    private fun validerReduksjonsdatoForInstitusjonsopphold(
        behandling: Behandling,
        nyeVurderinger: List<HelseinstitusjonVurderingDto>
    ): Validation<List<HelseinstitusjonVurderingDto>> {
        val grunnlag = helseinstitusjonRepository.hentHvisEksisterer(behandling.id)
        val opphold = grunnlag?.oppholdene?.opphold ?: emptyList()
        if (opphold.isEmpty() || nyeVurderinger.isEmpty()) return Validation.Valid(nyeVurderinger)

        val kjeder = grupperSammenhengendeOppholdSegmenter(opphold)

        // Håndterer når vedtatte vurderinger finnes. Dette skjer i revurdering
        val vurderingerPerKjede: Map<SammenhengendeOppholdGruppe, List<HelseinstitusjonVurderingDto>> =
            kjeder.associateWith { kjede ->
                nyeVurderinger
                    .filter { v -> v.periode.fom >= kjede.periode.fom && v.periode.tom <= kjede.periode.tom }
                    .sortedBy { it.periode }
            }

        // Henter forhåndsberegnet tidligste reduksjonsdato per kjede (bruker kjedens første segment som representant)
        // Alle representant-segmentene sendes inn samlet slik at 3-måneders-regelen mot forrige kjede beregnes riktig.
        val representantSegmentPerKjede = kjeder.associateWith { it.elementer.first() }
        val tidligsteReduksjonsdatoPerRepresentant =
            beregnTidligsteReduksjonsdatoPerOpphold(representantSegmentPerKjede.values.toList())
        val tidligsteReduksjonsdatoPerKjede = kjeder.associateWith { kjede ->
            tidligsteReduksjonsdatoPerRepresentant[representantSegmentPerKjede[kjede]]
        }

        vurderingerPerKjede.entries.forEach { (kjede, vurderinger) ->
            val tidligsteReduksjonsdato = tidligsteReduksjonsdatoPerKjede[kjede] ?: return@forEach
            val første = førsteReduksjonsvurdering(vurderinger)
            val resultat = validerReduksjonsdato(
                vurderinger,
                første,
                tidligsteReduksjonsdato
            )
            if (resultat != null) return resultat
        }

        return Validation.Valid(nyeVurderinger)
    }

    private fun førsteReduksjonsvurdering(vurderinger: List<HelseinstitusjonVurderingDto>): HelseinstitusjonVurderingDto? {
        return vurderinger.firstOrNull {
            it.faarFriKostOgLosji && it.forsoergerEktefelle == false && it.harFasteUtgifter == false
        }
    }


    private fun validerReduksjonsdato(
        vurderinger: List<HelseinstitusjonVurderingDto>,
        førsteReduksjonsvurdering: HelseinstitusjonVurderingDto?,
        tidligsteReduksjonsdato: LocalDate
    ): Validation<List<HelseinstitusjonVurderingDto>>? {
        if (førsteReduksjonsvurdering != null && førsteReduksjonsvurdering.periode.fom.isBefore(tidligsteReduksjonsdato)) {
            val tidligsteReduksjonsdatoFormatert =
                tidligsteReduksjonsdato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            return Validation.Invalid(
                vurderinger,
                "Reduksjonsvurdering starter for tidlig. Skal ikke starte før $tidligsteReduksjonsdatoFormatert"
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
        val vurdertAv: Bruker? = null,
        val vurdertTidspunkt: LocalDateTime? = null
    )
}