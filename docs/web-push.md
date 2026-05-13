# Web Push

LifePlanner usa Web Push con VAPID. Le chiavi non vanno committate: generarle una volta e configurarle come variabili d'ambiente.

```bash
npx web-push generate-vapid-keys
```

Variabili richieste:

```bash
APP_PUSH_VAPID_PUBLIC_KEY=...
APP_PUSH_VAPID_PRIVATE_KEY=...
APP_PUSH_VAPID_SUBJECT=mailto:admin@lifeplanner.local
```

In produzione mantenere stabile la coppia di chiavi. Cambiare chiavi richiede agli utenti di riattivare le notifiche.

Se l'app gira in Docker, esporre le variabili nel servizio backend:

```yaml
APP_PUSH_VAPID_PUBLIC_KEY: ${APP_PUSH_VAPID_PUBLIC_KEY}
APP_PUSH_VAPID_PRIVATE_KEY: ${APP_PUSH_VAPID_PRIVATE_KEY}
APP_PUSH_VAPID_SUBJECT: ${APP_PUSH_VAPID_SUBJECT}
```

Dopo aver modificato `.env`, ricreare il container backend per caricare le nuove variabili:

```bash
docker compose up -d --build backend
```

oppure:

```bash
docker compose up -d --force-recreate backend
```
