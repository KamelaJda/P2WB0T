# Kara
### Informacja
Wygląd kar

### Przykłady
```json
{
  "karaId": 2137,
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
  "dowody": "Lista dowodów (Format dowód jest na dole)",
  "punAktywna": "Wartość bezużyteczna :p"
}
```

#### Dowod
```json
{
  "id": 1,
  "user": "ID użytkownika",
  "content": "Treść dowodu (jeżeli wartość jest pusta, tego klucza nie będzie)",
  "image": "Link do zdjęcia (jeżeli wartość jest pusta, tego klucza nie będzie)"
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
