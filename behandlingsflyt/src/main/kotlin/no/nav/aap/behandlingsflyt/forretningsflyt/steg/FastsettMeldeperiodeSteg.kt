package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel.Companion.MELDEPERIODE_LENGDE
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettMeldeperiodeSteg(val sakRepository: SakRepository, val meldeperiodeRepository: MeldeperiodeRepository) :
    BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val rettighetsperiode = sakRepository.hent(kontekst.sakId).rettighetsperiode

        val gamlePerioder = meldeperiodeRepository.hentHvisEksisterer(kontekst.behandlingId)

        val meldeperioder = generateSequence(rettighetsperiode.fom) { it.plusDays(MELDEPERIODE_LENGDE) }
            .takeWhile { it <= rettighetsperiode.tom }
            .map { Periode(it, minOf(it.plusDays(MELDEPERIODE_LENGDE - 1), rettighetsperiode.tom)) }
            .toList()

        if (meldeperioder != gamlePerioder) {
            meldeperiodeRepository.lagre(kontekst.behandlingId, meldeperioder)
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val meldeperiodeRepository = repositoryProvider.provide<MeldeperiodeRepository>()
            return FastsettMeldeperiodeSteg(sakRepository, meldeperiodeRepository)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_MELDEPERIODER
        }
    }
}