# MessageLogs

## Path
`/api/chatmod/logs`

## Opis

Zwraca logi wiadomości

## Autoryzacja

Poprzez IP

## Query

| Key           | Value                                                                                                            |
|---------------|------------------------------------------------------------------------------------------------------------------|
| type          | Typ wyszukiwania (all, channel, user)                                                                            |
| data          | Nie może być puste! W przypadku wybrania `type` jako `channel` lub `user`, data ma wartość ID kanału/użytkownika |
| offset        | Offset (limit 10)                                                                                                                                    |

## Przykłady

* `/api/chatmod/logs?type=all&data=byle_co&offset=0` - To wyszuka ostatnie 10 usuniętych wiadomości
* `/api/chatmod/logs?type=user&data=id_usera&offset=0` - To wyszuka ostatnie 10 usuniętych wiadomości przez danego użytkownika
* `/api/chatmod/logs?type=channel&data=id_kanalu&offset=0` - To wyszuka ostatnie 10 usuniętych wiadomości w danym kanale

### Przykłady odpowiedzi
```json
{
  "success": false,
  "error": {
    "body": "Błąd!",
    "description": "Typ xyz jest błędny"
  } 
}
```

```json
{
  "success": true,
  "data": [
    {
      "message": {
        "id": "id wiadomości",
        "deletedDate": "czas usunięcia wiadomości",
        "createdDate": "czas stworzenia wiadomości",
        "userId": "ID autora",
        "channelId": "NAZWA kanału",
        "content": "treśc wiadomości"
      },
      "member": {
        "Patrz Userinfo.md"
      }
    },
    {
      "message": "..."
      "member": "..."
    }
  ]
}
```