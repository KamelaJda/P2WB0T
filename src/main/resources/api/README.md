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

1. Poprzez IP - nic nie trzeba dodać do patha/query/headerów
2. Poprzez Token - Trzeba dodać header `Authorization`, np. "Authorization: test123"

## Offset
Offset to po prostu podział na strony (żeby nie wyświetlać od razu 10k wartości). 
`Limit` - Ilość danych na stronę

### Przykład
Wykorzystanie offsetu w aplikacji react w przypadku, kiedy `limit` wynosi 10 (czyli 10 danych na stronę)

```jsx
const [data, setData] = useState();
const [offset, setOffset] = useState(0);
const [disabled, setDisabled] = useState(false);

const loadMore = () => {
    fetch("simple/path?offset=" + offset).then(json => { // omijam json.json() bo tak
        if (json.data.length < 10) setDisabled(true); // Jeżeli jest mniej niż 10 danych, to lista się kończy
        setOffset(offset + 10); // jest limit 10 danych na stronę, więc powiększamy offset o 10
        const j = data || [];
        j.push(json.data);
        setData(j);
    });
}

return (
  <div>
      <button 
        onClick={loadMore}
        disabled={disabled}
      >
          Załaduj więcej
      </button>{' '}
      <p>{JSON.stringify(data)}</p>
  </div
);

```