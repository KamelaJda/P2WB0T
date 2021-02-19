# Dokumentacja
Tutaj znajdują się endpointy do API bota. Rzeczy używane **tylko** na stronie bota nie są opisane, ponieważ nikomu to nie jest potrzebne /shurg

## Format odpowiedzi
Jeżeli odpowiedź będzie nie będzie OK, serwer zwróci
```json
{
  "success": false,
  "error": {
    "body": "Krótki opis błędu",
    "description": "Długi opis"
  }
}
```

Jeżeli odpowiedź będzie OK, możemy dostać dwa rodzaje odpowiedzi

Ten format jest używany najczęściej w POST, jeżeli serwer nie ma nic do zwrócenia
```json
{
  "success": true,
  "msg": "Opis zdarzenia"
}
```

Ten format natomiast jest używany, kiedy serwer chce zwrócić JSONObject'a.
Wartość `data` jest zależna od wysłanego requesta
```json
{
  "success": true,
  "data": {
    "..."
  }
}
```

## Autoryzacja
Wyróżniamy dwa sposoby autoryzacji requestów

1. Poprzez IP - nic nie trzeba dodać do patha/headerów
2. Poprzez Token - Trzeba dodać header `Authorization`, np. "Authorization: test123"