package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class EtAnnetStedUtlederService(connection: DBConnection) {
    private val barnetilleggRepository = BarnetilleggRepository(connection)
    private val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)

    fun harBehovForAvklaringer(behandlingId: BehandlingId): BehovForAvklaringer {
        val input = konstruerInput(behandlingId)

        return utledBehov(input)
    }

    internal fun utledBehov(input: EtAnnetStedInput): BehovForAvklaringer {
        val opphold = input.institusjonsOpphold
        val soningsOppgold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.FO }
        val helseopphold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.HS }
        val barnetillegg = input.barnetillegg

        if (soningsOppgold.isNotEmpty()) {
            return BehovForAvklaringer(true, false)
        }

        val helseOpphold = opprettTidslinje(helseopphold)
        val helseOppholdTidslinje = regnUtHelseinstitusjonsopphold(helseOpphold)

        val barnetilleggTidslinje = opprettTidslinje(barnetillegg.filter { it.personIdenter.isNotEmpty() }.map { segment ->
            Segment(
                segment.periode,
                true
            )
        })

        //fjern perioder hvor bruker har barnetillegg gjennom hele helseinstitusjonsoppholdet
        val oppholdUtenBarnetillegg =
            helseOppholdTidslinje.disjoint(barnetilleggTidslinje) { p, v -> Segment(p, v.verdi) }

        // Oppholdet må være lengre enn 3 måneder for å være aktuelt for avklaring og må ha vart i minimum 2 måneder for å være klar for avklaring
        val tremåneder = (3 * 30).toDuration(DurationUnit.DAYS)
        val oppholdSomKanGiReduksjon = harOppholdSomKreverAvklaring(oppholdUtenBarnetillegg, tremåneder)
        if (oppholdSomKanGiReduksjon.segmenter().isNotEmpty()) {
            return BehovForAvklaringer(false, true)
        }

        // Hvis det er mindre en 3 måneder siden sist opphold og bruker er nå innlagt
        if (harOppholdSomLiggerMindreEnnTreMånederFraForrigeSomGaReduksjon(helseOpphold.disjoint(barnetilleggTidslinje) { p, v ->
                Segment(
                    p,
                    v.verdi
                )
            }.komprimer(), oppholdSomKanGiReduksjon)
        ) {
            return BehovForAvklaringer(false, true)
        }

        return BehovForAvklaringer(false, false)
    }

    private fun harOppholdSomLiggerMindreEnnTreMånederFraForrigeSomGaReduksjon(
        helseOpphold: Tidslinje<Boolean>,
        oppholdUtenBarnetillegg: Tidslinje<Boolean>
    ): Boolean = helseOpphold.segmenter()
        .filter { segment -> segment.verdi }
        .any { segment ->
            segment.periode.tom >= LocalDate.now() && oppholdUtenBarnetillegg.segmenter()
                .any { segment.periode.fom.minusMonths(3) <= it.periode.tom }
        }

    private fun harOppholdSomKreverAvklaring(
        oppholdUtenBarnetillegg: Tidslinje<Boolean>,
        femMåneder: Duration
    ): Tidslinje<Boolean> {
        return Tidslinje(oppholdUtenBarnetillegg.segmenter()
            .filter { segment -> segment.verdi }
            .filterNotNull()
            .filter { segment ->
                harOppholdSomVarerMerEnnFireMånederOgErMinstToMånederInnIOppholdet(segment, femMåneder)
            })
    }

    private fun harOppholdSomVarerMerEnnFireMånederOgErMinstToMånederInnIOppholdet(
        segment: Segment<Boolean>,
        femMåneder: Duration
    ): Boolean {
        val lengde = segment.periode.antallDager().toDuration(DurationUnit.DAYS)
        return femMåneder < lengde && (segment.fom().plusMonths(2) < LocalDate.now() || Miljø.er() == MiljøKode.DEV)
    }

    private fun regnUtHelseinstitusjonsopphold(helseOpphold: Tidslinje<Boolean>): Tidslinje<Boolean> {

        // Fjerne innleggelsesmåneden, dvs første måned i hvert "opphold"
        val helseOppholdTidslinje = Tidslinje(
            helseOpphold.segmenter()
                .map { segment -> justerForInnleggelsesMåned(segment) }
                .filterNotNull()
        )
        return helseOppholdTidslinje
    }

    private fun justerForInnleggelsesMåned(segment: Segment<Boolean>): Segment<Boolean>? {
        val fom = segment.fom().withDayOfMonth(1).plusMonths(1)

        if (fom.isAfter(segment.tom())) {
            return null
        }
        return Segment(Periode(fom, segment.tom()), segment.verdi)
    }

    private fun <T> opprettTidslinje(segmenter: List<Segment<T>>): Tidslinje<Boolean> {
        return segmenter.sortedBy { it.fom() }.map { segment ->
            Tidslinje(
                segment.periode,
                true
            )
        }.fold(Tidslinje<Boolean>()) { acc, tidslinje ->
            acc.kombiner(tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }.komprimer()
    }

    private fun konstruerInput(behandlingId: BehandlingId): EtAnnetStedInput {
        val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)
        val barnetillegg = barnetilleggRepository.hentHvisEksisterer(behandlingId)?.perioder ?: emptyList()

        val opphold = grunnlag?.opphold ?: emptyList()

        return EtAnnetStedInput(opphold, barnetillegg)
    }
}