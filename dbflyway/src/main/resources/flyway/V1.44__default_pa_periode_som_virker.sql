UPDATE helseinstitusjon_vurdering
SET periode = daterange('2000-01-01', '2000-01-01', '[]')
WHERE periode = 'empty';