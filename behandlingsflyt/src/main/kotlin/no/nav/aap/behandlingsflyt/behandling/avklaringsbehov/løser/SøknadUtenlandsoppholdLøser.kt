package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BekreftBrevSendtLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderSøknadUtenlandsoppholdLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.utenlandsopphold.BekreftBrevSendtBekreftelse
import no.nav.aap.utenlandsopphold.BekreftBrevSendtRepository
import no.nav.aap.utenlandsopphold.SøknadUtenlandsoppholdRepository
import no.nav.aap.utenlandsopphold.SøknadUtenlandsoppholdVurdering
import java.time.LocalDateTime

class VurderSøknadUtenlandsoppholdLøser(repositoryProvider: RepositoryProvider) :
    AvklaringsbehovsLøser<VurderSøknadUtenlandsoppholdLøsning> {
    private val repository = repositoryProvider.provide<SøknadUtenlandsoppholdRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: VurderSøknadUtenlandsoppholdLøsning
    ): LøsningsResultat {
        val dto = løsning.vurdering
        SøknadUtenlandsoppholdVurdering.valider(
            søknadsperiode = dto.søknadsperiode,
            land = dto.land,
            resultat = dto.resultat,
            godkjentPeriode = dto.godkjentPeriode,
            antallGodkjenteDager = dto.antallGodkjenteDager,
            begrunnelse = dto.begrunnelse,
        )?.let { throw UgyldigForespørselException(it) }

        repository.lagre(
            kontekst.behandlingId(),
            dto.tilVurdering(vurdertAv = kontekst.bruker.ident, vurdertTidspunkt = LocalDateTime.now())
        )
        return LøsningsResultat("Vurdert søknad om opphold i utlandet")
    }

    override fun forBehov(): Definisjon = Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD
}

class BekreftBrevSendtLøser(repositoryProvider: RepositoryProvider) :
    AvklaringsbehovsLøser<BekreftBrevSendtLøsning> {
    private val repository = repositoryProvider.provide<BekreftBrevSendtRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: BekreftBrevSendtLøsning
    ): LøsningsResultat {
        if (!løsning.bekreftelse.brevSendt) {
            throw UgyldigForespørselException("Brevet må være sendt før behandlingen kan avsluttes.")
        }

        repository.lagre(
            kontekst.behandlingId(),
            BekreftBrevSendtBekreftelse(
                brevSendt = true,
                bekreftetAv = kontekst.bruker.ident,
                bekreftetTidspunkt = LocalDateTime.now(),
            )
        )
        return LøsningsResultat("Bekreftet at brev er sendt")
    }

    override fun forBehov(): Definisjon = Definisjon.BEKREFT_BREV_SENDT_MANUELT
}
