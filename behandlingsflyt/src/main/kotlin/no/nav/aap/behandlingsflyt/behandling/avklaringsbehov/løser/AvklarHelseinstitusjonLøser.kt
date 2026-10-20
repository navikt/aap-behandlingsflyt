package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.SammenhengendeOppholdGruppe
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.beregnTidligsteReduksjonsdatoPerKjede
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.giReduksjon
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.grupperSammenhengendeOppholdSegmenter
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.lagOppholdId
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.gateway.GatewayProvider
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
    private val helseinstitusjonRepository: InstitusjonsoppholdRepository,
    private val unleashGateway: UnleashGateway
) : AvklaringsbehovsLøser<AvklarHelseinstitusjonLøsning> {
    private val enManedsregelEnabled get() = unleashGateway.isEnabled(BehandlingsflytFeature.Helseinstitusjon1Manedsregel)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        helseinstitusjonRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarHelseinstitusjonLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())
        val vurdertAv = kontekst.bruker

        val innsendteVurderinger =
            if (enManedsregelEnabled) {
                løsning.helseinstitusjonVurdering.vurderinger
            } else {
                løsning.helseinstitusjonVurdering.vurderinger.map { it.copy(erHistoriskUtenReduksjonsberegning = false) }
            }

        val oppdaterteVurderinger =
            slåSammenMedNyeVurderinger(
                innsendteVurderinger,
                behandling,
                vurdertAv
            )

        validerReduksjonsdatoForInstitusjonsopphold(
            behandling,
            if (enManedsregelEnabled) {
                innsendteVurderinger.filterNot { it.erHistoriskUtenReduksjonsberegning }
            } else {
                innsendteVurderinger
            },
            oppdaterteVurderinger,
            løsning.helseinstitusjonVurdering.vurderinger
        ).throwOnInvalid {
            UgyldigForespørselException(it.errorMessage)
        }

        helseinstitusjonRepository.lagreHelseVurdering(
            kontekst.kontekst.behandlingId,
            oppdaterteVurderinger,
            sammenhengendeOppholdEnabled = unleashGateway.isEnabled(BehandlingsflytFeature.SammenhengendeInstitusjonsopphold)
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
        val nåværendeGrunnlag = helseinstitusjonRepository.hentHvisEksisterer(behandling.id)

        val (nyeHistoriske, nyePeriodiserte) =
            if (enManedsregelEnabled) nyeVurderinger.partition { it.erHistoriskUtenReduksjonsberegning }
            else emptyList<HelseinstitusjonVurderingDto>() to nyeVurderinger

        val nåværendeOpphold = nåværendeGrunnlag?.oppholdene?.opphold.orEmpty()
        val historiskeOppholdPerioder = nyeHistoriske.mapNotNull { historisk ->
            nåværendeOpphold.firstOrNull { lagOppholdId(it.verdi.navn, it.periode.fom) == historisk.oppholdId }
                ?: if (historisk.oppholdId == null) {
                    nåværendeOpphold.firstOrNull { it.periode.overlapper(historisk.periode) }
                } else {
                    null
                }
        }.map { it.periode }

        val eksisterendeTidslinje =
            byggTidslinjeForHelseoppholdvurderingerBegrensetTilOpphold(forrigeGrunnlag, nåværendeGrunnlag)
                .fjernReduksjonIPerioder(historiskeOppholdPerioder)

        val oppholdIderMedNyInput = nyeVurderinger.mapNotNull { it.oppholdId }.toSet()
        val nåværendeOppholdIder = nåværendeOpphold.map { lagOppholdId(it.verdi.navn, it.periode.fom) }.toSet()
        val eksisterendeHistoriske = forrigeGrunnlag?.helseoppholdvurderinger?.historiskeVurderinger.orEmpty()
            .filter { it.oppholdId !in oppholdIderMedNyInput }
            .filter { nåværendeOpphold.isEmpty() || it.oppholdId in nåværendeOppholdIder }

        val periodiserte = if (nyePeriodiserte.isEmpty()) {
            eksisterendeTidslinje.segmenter().map { it.tilVurdering(it.verdi.vurdertAv) }
        } else {
            val nyTidslinje = Tidslinje(nyePeriodiserte.sortedBy { it.periode }
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
                            vurdertTidspunkt = LocalDateTime.now(),
                            erHistoriskUtenReduksjonsberegning = false,
                            oppholdId = it.oppholdId,
                        )
                    )
                }).komprimer()

            eksisterendeTidslinje.kombiner(nyTidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
                .komprimer()
                .segmenter()
                .map { it.tilVurdering(vurdertAv) }
        }

        val historiske = eksisterendeHistoriske + nyeHistoriske.map {
            HelseinstitusjonVurdering(
                begrunnelse = it.begrunnelse,
                faarFriKostOgLosji = it.faarFriKostOgLosji,
                forsoergerEktefelle = it.forsoergerEktefelle,
                harFasteUtgifter = it.harFasteUtgifter,
                periode = it.periode,
                vurdertIBehandling = behandling.id,
                vurdertAv = vurdertAv,
                vurdertTidspunkt = LocalDateTime.now(),
                erHistoriskUtenReduksjonsberegning = true,
                oppholdId = it.oppholdId,
            )
        }

        return periodiserte + historiske
    }

    private fun Tidslinje<HelseoppholdVurderingData>.fjernReduksjonIPerioder(
        perioder: List<Periode>
    ): Tidslinje<HelseoppholdVurderingData> {
        if (perioder.isEmpty()) return this

        val gjenværende = segmenter().flatMap { segment ->
            if (!segment.verdi.girReduksjon()) {
                listOf(segment)
            } else {
                segment.periode.trekkFra(perioder).map { Segment(it, segment.verdi) }
            }
        }

        return Tidslinje(gjenværende.sortedBy { it.periode })
    }

    private fun HelseoppholdVurderingData.girReduksjon(): Boolean =
        !erHistoriskUtenReduksjonsberegning &&
                faarFriKostOgLosji &&
                forsoergerEktefelle == false &&
                harFasteUtgifter == false

    private fun Periode.trekkFra(andre: List<Periode>): List<Periode> =
        andre.sortedBy { it.fom }.fold(listOf(this)) { rest, fjern ->
            rest.flatMap { p ->
                if (!p.overlapper(fjern)) {
                    listOf(p)
                } else {
                    listOfNotNull(
                        if (p.fom < fjern.fom) Periode(p.fom, fjern.fom.minusDays(1)) else null,
                        if (p.tom > fjern.tom) Periode(fjern.tom.plusDays(1), p.tom) else null,
                    )
                }
            }
        }

    private fun Segment<HelseoppholdVurderingData>.tilVurdering(vurdertAv: Bruker?) = HelseinstitusjonVurdering(
        begrunnelse = verdi.begrunnelse,
        faarFriKostOgLosji = verdi.faarFriKostOgLosji,
        forsoergerEktefelle = verdi.forsoergerEktefelle,
        harFasteUtgifter = verdi.harFasteUtgifter,
        periode = periode,
        vurdertIBehandling = verdi.vurdertIBehandling,
        vurdertAv = vurdertAv,
        vurdertTidspunkt = verdi.vurdertTidspunkt,
        erHistoriskUtenReduksjonsberegning = false,
        oppholdId = verdi.oppholdId,
    )

    private fun byggTidslinjeForHelseoppholdvurderingerBegrensetTilOpphold(
        forrigeGrunnlag: InstitusjonsoppholdGrunnlag?,
        nåværendeGrunnlag: InstitusjonsoppholdGrunnlag?
    ): Tidslinje<HelseoppholdVurderingData> {
        val tidslinje = forrigeGrunnlag?.helseoppholdvurderinger?.tilTidslinje()
            ?.mapValue {
                HelseoppholdVurderingData(
                    begrunnelse = it.begrunnelse,
                    faarFriKostOgLosji = it.faarFriKostOgLosji,
                    forsoergerEktefelle = it.forsoergerEktefelle,
                    harFasteUtgifter = it.harFasteUtgifter,
                    vurdertIBehandling = it.vurdertIBehandling,
                    vurdertAv = it.vurdertAv,
                    vurdertTidspunkt = it.vurdertTidspunkt,
                    erHistoriskUtenReduksjonsberegning = it.erHistoriskUtenReduksjonsberegning,
                    oppholdId = it.oppholdId
                )
            }.orEmpty()

        val forrigeOpphold = forrigeGrunnlag?.oppholdene?.opphold.orEmpty()
        val nåværendeOpphold = nåværendeGrunnlag?.oppholdene?.opphold.orEmpty()

        // Mangler vi oppholdsgrunnlag for enten forrige eller nåværende, behold gammel tidslinje uendret
        if (forrigeOpphold.isEmpty() || nåværendeOpphold.isEmpty()) return tidslinje

        // Match hvert nåværende opphold mot forrige opphold med SAMME fom (startdato).
        // Dette identifiserer om oppholdet er en videreføring (forkortet/forlenget) av et
        // eksisterende opphold, eller om det er et helt nytt opphold som tilfeldigvis
        // ligger innenfor den gamle oppholdets (evt. løpende) tidsrom.
        val gjenkjenteOppholdsperioder = nåværendeOpphold.mapNotNull { nå ->
            val matchendeForrige = forrigeOpphold.find { it.periode.fom == nå.periode.fom }
            matchendeForrige?.let {
                Periode(nå.periode.fom, minOf(it.periode.tom, nå.periode.tom))
            }
        }

        if (gjenkjenteOppholdsperioder.isEmpty()) return Tidslinje(emptyList())

        val gjenkjentTidslinje = gjenkjenteOppholdsperioder
            .map { Tidslinje(it, Unit) }
            .fold(Tidslinje<Unit>()) { acc, t -> acc.kombiner(t, StandardSammenslåere.prioriterHøyreSideCrossJoin()) }
            .komprimer()

        return tidslinje.kombiner(gjenkjentTidslinje, StandardSammenslåere.kunVenstre())
            .map { it.copy(vurdertTidspunkt = LocalDateTime.now()) }
            .komprimer()
    }

    private fun validerReduksjonsdatoForInstitusjonsopphold(
        behandling: Behandling,
        nyeVurderinger: List<HelseinstitusjonVurderingDto>,
        oppdaterteVurderinger: List<HelseinstitusjonVurdering>,
        alleInnsendte: List<HelseinstitusjonVurderingDto>
    ): Validation<List<HelseinstitusjonVurderingDto>> {
        val opphold = helseinstitusjonRepository.hentHvisEksisterer(behandling.id)?.oppholdene?.opphold.orEmpty()
        if (opphold.isEmpty() || (nyeVurderinger.isEmpty() && alleInnsendte.isEmpty())) {
            return Validation.Valid(nyeVurderinger)
        }

        val kjeder = grupperSammenhengendeOppholdSegmenter(opphold)
        val tidligsteReduksjonsdatoPerKjede = beregnTidligsteReduksjonsdatoPerKjede(
            opphold,
            harInnvilgetReduksjon(oppdaterteVurderinger),
            enManedsregelEnabled
        )

        validerHistoriskeVurderinger(alleInnsendte, kjeder, tidligsteReduksjonsdatoPerKjede)?.let { return it }
        validerReduksjonsdatoerPerKjede(nyeVurderinger, kjeder, tidligsteReduksjonsdatoPerKjede)?.let { return it }

        return Validation.Valid(nyeVurderinger)
    }

    private fun harInnvilgetReduksjon(
        oppdaterteVurderinger: List<HelseinstitusjonVurdering>
    ): (SammenhengendeOppholdGruppe) -> Boolean = { kjede ->
        oppdaterteVurderinger.any { it.giReduksjon() && it.periode.overlapper(kjede.periode) }
    }

    private fun førsteReduksjonsvurdering(vurderinger: List<HelseinstitusjonVurderingDto>): HelseinstitusjonVurderingDto? {
        return vurderinger.firstOrNull { it.giReduksjon() }
    }

    private fun validerHistoriskeVurderinger(
        alleInnsendte: List<HelseinstitusjonVurderingDto>,
        kjeder: List<SammenhengendeOppholdGruppe>,
        tidligsteReduksjonsdatoPerKjede: Map<SammenhengendeOppholdGruppe, LocalDate>
    ): Validation<List<HelseinstitusjonVurderingDto>>? {
        if (!enManedsregelEnabled) return null

        val harFeilaktigHistorisk = alleInnsendte
            .filter { it.erHistoriskUtenReduksjonsberegning && it.girReduksjonUtenHistorikk() }
            .any { rekkerInnenforOppholdet(it, kjeder, tidligsteReduksjonsdatoPerKjede) }

        return if (harFeilaktigHistorisk) {
            Validation.Invalid(
                alleInnsendte,
                "Reduksjon kan ikke lagres som historikk når den rekker innenfor oppholdet"
            )
        } else {
            null
        }
    }

    private fun HelseinstitusjonVurderingDto.girReduksjonUtenHistorikk(): Boolean =
        faarFriKostOgLosji && forsoergerEktefelle == false && harFasteUtgifter == false

    private fun rekkerInnenforOppholdet(
        vurdering: HelseinstitusjonVurderingDto,
        kjeder: List<SammenhengendeOppholdGruppe>,
        tidligsteReduksjonsdatoPerKjede: Map<SammenhengendeOppholdGruppe, LocalDate>
    ): Boolean {
        val kjede = kjeder.firstOrNull { it.inneholderOpphold(vurdering.oppholdId) } ?: return false
        val tidligste = tidligsteReduksjonsdatoPerKjede[kjede] ?: return false
        return !tidligste.isAfter(kjede.periode.tom)
    }

    private fun SammenhengendeOppholdGruppe.inneholderOpphold(oppholdId: String?): Boolean =
        elementer.any { lagOppholdId(it.verdi.navn, it.periode.fom) == oppholdId }

    private fun validerReduksjonsdatoerPerKjede(
        nyeVurderinger: List<HelseinstitusjonVurderingDto>,
        kjeder: List<SammenhengendeOppholdGruppe>,
        tidligsteReduksjonsdatoPerKjede: Map<SammenhengendeOppholdGruppe, LocalDate>
    ): Validation<List<HelseinstitusjonVurderingDto>>? {
        kjeder.forEach { kjede ->
            val tidligsteReduksjonsdato = tidligsteReduksjonsdatoPerKjede[kjede] ?: return@forEach
            val vurderingerForKjede = nyeVurderinger
                .filter { it.periode.fom >= kjede.periode.fom && it.periode.tom <= kjede.periode.tom }
                .sortedBy { it.periode }
            val resultat = validerReduksjonsdato(
                vurderingerForKjede,
                førsteReduksjonsvurdering(vurderingerForKjede),
                tidligsteReduksjonsdato
            )
            if (resultat != null) return resultat
        }
        return null
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
        val vurdertTidspunkt: LocalDateTime? = null,
        val erHistoriskUtenReduksjonsberegning: Boolean = false,
        val oppholdId: String? = null,
    )
}