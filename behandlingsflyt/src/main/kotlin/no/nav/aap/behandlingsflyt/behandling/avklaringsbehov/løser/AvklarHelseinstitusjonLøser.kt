package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.PeriodisertInstitusjonsoppholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
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
        val vurdertAv = kontekst.bruker.ident

        validerReduksjonsdatoForInstitusjonsopphold(
            behandling,
            løsning.løsningerForPerioder
        ).throwOnInvalid {
            UgyldigForespørselException(it.errorMessage)
        }

        val vedtatteVurderinger =
            behandling.forrigeBehandlingId?.let { helseinstitusjonRepository.hentHvisEksisterer(it) }

        val oppdaterteVurderinger = slåSammenMedNyeVurderinger(
            vedtatteVurderinger,
            løsning.løsningerForPerioder,
            behandling,
            vurdertAv
        )

        helseinstitusjonRepository.lagreHelseVurdering(
            kontekst.kontekst.behandlingId,
            vurdertAv,
            oppdaterteVurderinger
        )

        return LøsningsResultat(løsning.løsningerForPerioder.joinToString(" ") { it.begrunnelse })
    }

    private fun slåSammenMedNyeVurderinger(
        grunnlag: InstitusjonsoppholdGrunnlag?,
        nyeVurderinger: List<PeriodisertInstitusjonsoppholdDto>,
        behandling: Behandling,
        vurdertAv: String,
    ): List<HelseinstitusjonVurdering> {
        val eksisterendeTidslinje = byggTidslinjeForHelseoppholdvurderinger(grunnlag)

        val nyeVurderingerTidslinje = Tidslinje(nyeVurderinger.sortedBy { it.fom }
            .map {
                val periode = Periode(it.fom, checkNotNull(it.tom) { "tom må være satt for institusjonsoppholdsvurdering" })
                Segment(
                    periode,
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

    private fun byggTidslinjeForHelseoppholdvurderinger(grunnlag: InstitusjonsoppholdGrunnlag?): Tidslinje<HelseoppholdVurderingData> {
        return grunnlag?.helseoppholdvurderinger?.tilTidslinje()
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
        behandling: Behandling,
        nyeVurderinger: List<PeriodisertInstitusjonsoppholdDto>
    ): Validation<List<PeriodisertInstitusjonsoppholdDto>> {
        val grunnlag = helseinstitusjonRepository.hentHvisEksisterer(behandling.id)
        val opphold = grunnlag?.oppholdene?.opphold ?: emptyList()
        if (opphold.isEmpty() || nyeVurderinger.isEmpty()) return Validation.Valid(nyeVurderinger)

        val vurderingerPerOpphold: Map<Segment<Institusjon>, List<PeriodisertInstitusjonsoppholdDto>> =
            opphold.associateWith { o ->
                nyeVurderinger
                    .filter { v -> v.fom >= o.periode.fom && (v.tom == null || v.tom <= o.periode.tom) }
                    .sortedBy { it.fom }
            }

        val resultatFørste = validerFørsteOpphold(vurderingerPerOpphold)
        if (resultatFørste != null) return resultatFørste

        val resultatPåfølgende = validerPåfølgendeOpphold(vurderingerPerOpphold)
        if (resultatPåfølgende != null) return resultatPåfølgende

        return Validation.Valid(nyeVurderinger)
    }

    private fun validerFørsteOpphold(
        vurderingerPerOpphold: Map<Segment<Institusjon>, List<PeriodisertInstitusjonsoppholdDto>>
    ): Validation<List<PeriodisertInstitusjonsoppholdDto>>? {
        vurderingerPerOpphold.entries.firstOrNull()?.let { (opphold, vurderinger) ->
            val første = førsteReduksjonsvurdering(vurderinger)
            val tidligsteReduksjonsdato = opphold.periode.fom.withDayOfMonth(1).plusMonths(4)
            return validerReduksjonsdato(
                vurderinger, første, tidligsteReduksjonsdato,
                "Første reduksjonsvurdering starter for tidlig. Skal ikke starte før"
            )
        }
        return null
    }

    private fun validerPåfølgendeOpphold(
        vurderingerPerOpphold: Map<Segment<Institusjon>, List<PeriodisertInstitusjonsoppholdDto>>
    ): Validation<List<PeriodisertInstitusjonsoppholdDto>>? {
        if (vurderingerPerOpphold.size > 1) {
            vurderingerPerOpphold.entries.toList()
                .windowed(2)
                .forEach { (forrige, nåværende) ->
                    val forrigeOpphold = forrige.key
                    val nåværendeOpphold = nåværende.key
                    val vurderingerNåværende = nåværende.value

                    val treMånederEtterUtskrivelse = forrigeOpphold.periode.tom.plusMonths(3)
                    val erInnenTreMåneder = !nåværendeOpphold.periode.fom.isAfter(treMånederEtterUtskrivelse)
                    val første = førsteReduksjonsvurdering(vurderingerNåværende)

                    if (!erInnenTreMåneder) {
                        val tidligsteReduksjonsdato = nåværendeOpphold.periode.fom.withDayOfMonth(1).plusMonths(4)
                        val resultat = validerReduksjonsdato(
                            vurderingerNåværende, første, tidligsteReduksjonsdato,
                            "Reduksjon ved nytt opphold starter for tidlig. Skal ikke starte før"
                        )
                        if (resultat != null) return resultat
                    }
                }
        }
        return null
    }

    private fun førsteReduksjonsvurdering(vurderinger: List<PeriodisertInstitusjonsoppholdDto>): PeriodisertInstitusjonsoppholdDto? {
        return vurderinger.firstOrNull {
            it.faarFriKostOgLosji && it.forsoergerEktefelle == false && it.harFasteUtgifter == false
        }
    }

    private fun validerReduksjonsdato(
        vurderinger: List<PeriodisertInstitusjonsoppholdDto>,
        førsteReduksjonsvurdering: PeriodisertInstitusjonsoppholdDto?,
        tidligsteReduksjonsdato: LocalDate,
        feilmelding: String
    ): Validation<List<PeriodisertInstitusjonsoppholdDto>>? {
        if (førsteReduksjonsvurdering != null && førsteReduksjonsvurdering.fom.isBefore(tidligsteReduksjonsdato)) {
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
        val vurdertIBehandling: BehandlingId? = null,
        val vurdertAv: String? = null,
        val vurdertTidspunkt: LocalDateTime? = null
    )
}