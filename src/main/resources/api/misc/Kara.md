# Kara
### Informacja
Wygląd kary

### Przykłady
```json
{
  "karaId": 1237,
  "karany": "Patrz Userinfo.md",
  "mcNick": "Nick w mc, na którym został ukarany",
  "adm": "Patrz Userinfo.md",
  "powod": "Powód kary",
  "timestamp": "Czas nadania kary. Format w longu!",
  "typKary": "Patrz TypKary na dole",
  "aktywna": false,
  "messageUrl": "URL wiadomości w formacie 'id serwera/id kanału/id wiadomości'",
  "end": "Kiedy koniec kary. Format w longu!",
  "duration": "Na ile nadano (2h, 1d, 2137m)",
  "punAktywna": "Wartość bezużyteczna :p",
  "dowody": "Lista dowodów (Format dowód jest na dole)"
}
```

#### Dowod
```json
{
  "id": 1,
  "user": "ID użytkownika",
  "content": "Treść dowodu (jeżeli nie ma, tego klucza nie będzie)",
  "image": "Link do zdjęcia (jeżeli nie ma, tego klucza nie będzie)"
}
```

#### Typ Kary
Możliwe typy kar

| Typ      |
|----------|
| KICK     |
| BAN      |
| MUTE     |
| TEMPBAN  |
| TEMPMUTE |
| UNMUTE   |
| UNBAN    |
