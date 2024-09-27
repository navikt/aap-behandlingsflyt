package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate

class EtAnnetStedUtlederService(connection: DBConnection) {
    private val barnetilleggRepository = BarnetilleggRepository(connection)
    private val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)

    fun harBehovForAvklaringer(behandlingId: BehandlingId): BehovForAvklaringer {
        val input = konstruerInput(behandlingId)

        return utledBehov(input)
    }

    private fun utledBehov(input: EtAnnetStedInput): BehovForAvklaringer {
        val opphold = input.institusjonsOpphold
        val soningsOppgold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.FO }
        val helseopphold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.HS }
        val barnetillegg = input.barnetillegg

        // Oppholdet må være lengre enn 3 måneder for å være aktuelt for avklaring
        val lengreOpphold = helseopphold.filter { segment ->
            val startTelling = LocalDate.of(segment.fom().year, segment.fom().month.plus(1).value,1)
            val kretivPeriode = startTelling.plusMonths(3) < segment.periode.fom

            return@filter kretivPeriode
        }

        // Oppholdet må være lengre enn 2 måneder for å være aktuelt for avklaring
        val klarForAvklaringer = lengreOpphold.filter { segment ->
            val startTelling = LocalDate.of(segment.fom().year, segment.fom().month.plus(1).value,1)
            val toMånederInnIOppholdet = startTelling.plusMonths(2) >= LocalDate.now()

            return@filter toMånederInnIOppholdet
        }

        // Dersom bruker har barnetillegg i perioden for innleggelse, er det ikke behov for avklaring
        val harIkkeBarnetillegg = klarForAvklaringer.filter { segment ->
            return@filter barnetillegg.map { it.periode }.none { it.overlapper(segment.periode) }
        }


        return BehovForAvklaringer(input.harUavklartSoningsforhold(), klarForAvklaringer.isNotEmpty())
    }

    private fun konstruerInput(behandlingId: BehandlingId): EtAnnetStedInput {
        val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)
        val barnetillegg = barnetilleggRepository.hentHvisEksisterer(behandlingId)?.perioder ?: emptyList()

        val opphold = grunnlag?.opphold ?: emptyList()

        return EtAnnetStedInput(opphold, barnetillegg)
    }
}