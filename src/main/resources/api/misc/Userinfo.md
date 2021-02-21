# Userinfo
### Informacja
Userinfo jest używane, kiedy serwer chce zwrócić użytkownika Discorda. 

### Przykłady
Jeżeli użytkownik jest na serwerze
```json
{
  "id": "id użytkownika",
  "mcNick": "nick na serwerze Discord (jeżeli użytkownik nie ma ustawionego nicku, nie ma tego klucza)",
  "username": "nick konta na discordzie",
  "tag": "tag użytkownika (np. #2137)",
  "permLevel": "permisje użytkownika (patrz PermLevel.md)",
  "avatarUrl": "pełny URL do avataru",
  "inGuild": "BOOLEAN, czy użytkownik jest na serwerze",
  "roles": ["lista", "ról", "użytkownika"]
}
```

Jeżeli użytkownika nie ma na serwerze, otrzymamy to
```json
{
  "id": "id użytkownika",
  "username": "nick konta na discordzie",
  "tag": "tak użytkownika (np. #2137)",
  "avatarUrl": "pełny URL do avataru"
}
```
