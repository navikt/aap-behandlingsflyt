package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.behandling.underveis.UnderveisService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.PliktkortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class UnderveisSteg(private val underveisService: UnderveisService) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(UnderveisSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val underveisTidslinje = underveisService.vurder(kontekst.behandlingId)

        log.info("Underveis tidslinje $underveisTidslinje")

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val behandlingService = SakOgBehandlingService(connection)
            return UnderveisSteg(
                UnderveisService(
                    behandlingService = behandlingService,
                    vilkårsresultatRepository = VilkårsresultatRepository(connection),
                    pliktkortRepository = PliktkortRepository(connection),
                    underveisRepository = UnderveisRepository(connection),
                    aktivitetspliktRepository = AktivitetspliktRepository(connection),
                    etAnnetStedUtlederService = EtAnnetStedUtlederService(
                        BarnetilleggRepository(connection),
                        InstitusjonsoppholdRepository(connection),
                        behandlingService
                    ),
                    arbeidsevneRepository = ArbeidsevneRepository(connection),
                    meldepliktRepository = MeldepliktRepository(connection),
                )
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_UTTAK
        }
    }
}