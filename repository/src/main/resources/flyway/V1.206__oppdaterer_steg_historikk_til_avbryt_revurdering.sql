update steg_historikk
set steg = 'AVBRYT_REVURDERING'
where steg = 'KANSELLER_REVURDERING';
