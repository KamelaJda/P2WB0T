# CheckToken

## Path
`api/util/checktoken`

## Opis

Zwraca prawidłowość tokenu

## Headers
| Key           | Value   |
|---------------|---------|
| Authorization | API Key |

## Przykłady
```json
{
  "success": true,
  "msg": "Token jest dobry"
}
```

```json
{
  "success": false,
  "error": {
    "body": "Brak autoryzacji",
    "description": "Token jest nieprawidłowy."
  }
}
```