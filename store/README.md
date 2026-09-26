# Play Store package

Everything that goes into the Play Console by hand lives here. `tools/check_store.py`
checks the texts (character limits, all 14 languages present, invisible characters);
CI runs it on every push.

```
play/<lang>/title.txt           app name              ≤ 30 characters
play/<lang>/short.txt           short description     ≤ 80
play/<lang>/full.txt            full description      ≤ 4000
play/<lang>/notes-<version>.txt release notes         ≤ 500
graphics/icon-512.png           high-res icon (512×512)
graphics/feature-1024.png       feature graphic (1024×500); no text, same in every language
screenshots/en/                 phone screenshots, English only (1080×2160: Play requires
                                the long side to be at most 2× the short side;
                                status and navigation bars cropped)
data-safety.md                  answers for the Data safety form
content-rating.md               answers for Content rating (IARC) and Target audience
checklist.md                    release steps
```

Graphics are generated from SVG: `inkscape graphics/icon-512.svg --export-type=png -w 512 -h 512 …`

## Language folder → Play Console locale

| Folder | Play | Folder | Play |
|---|---|---|---|
| `en` | English (United States) – en-US | `it` | Italian – it-IT |
| `tr` | Turkish – tr-TR | `da` | Danish – da-DK |
| `de` | German – de-DE | `sv` | Swedish – sv-SE |
| `fr` | French (France) – fr-FR | `nb` | Norwegian – no-NO |
| `nl` | Dutch – nl-NL | `fi` | Finnish – fi-FI |
| `es` | Spanish (Spain) – es-ES | `ru` | Russian – ru-RU |
| `pt` | Portuguese (Brazil) – pt-BR | `ar` | Arabic – ar |

The default language is **en-US**. Release notes are pasted into Play in a single box,
wrapped in language tags (`<tr-TR> … </tr-TR>`). Each language's text lives in its own
`notes-<version>.txt` file.
