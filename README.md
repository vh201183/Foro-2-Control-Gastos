
# ComposeExpenses (Firebase)

App de control de gastos con **Jetpack Compose**, **Firebase Auth (correo + Google)** y **Firestore**.

## Configuración rápida
1. Registra la app Android con package **`com.example.composegrades`**.
2. Descarga `google-services.json` y colócalo en `app/`.
3. En **Authentication > Sign-in method** habilita **Email/Password** y **Google**.
4. Agrega **SHA-1 y SHA-256** (`signingReport`) en **Project settings > Your apps > Android**.
5. Sincroniza y ejecuta.

> Usa **`R.string.default_web_client_id`** del `google-services.json` (no necesitas strings manuales).

## Firestore
Colección: `/users/{uid}/expenses/{expenseId}`.

Reglas ejemplo:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid}/expenses/{doc} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```
