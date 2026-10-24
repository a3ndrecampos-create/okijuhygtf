# Arka - Cálculo de Ganhos

App Android (Kotlin + Jetpack Compose) que lê a oferta de corrida na tela do Uber, 99 e inDrive
e mostra um semáforo (verde / amarelo / vermelho) com R$/km, R$/hora e lucro real.

## Gerar o APK no GitHub
1. Crie um repositório e envie todos os arquivos (inclusive a pasta `.github`).
2. Aba **Actions** > **Build APK** > **Run workflow** (também roda a cada push). Os testes rodam antes do build.
3. Baixe o artefato **Arka-APK** (`Arka-debug.apk` e `Arka-release.apk`).
4. Para publicar uma versão: crie uma tag `v1.1.0` e o APK vai para a aba **Releases**.

## Instalar e ativar
1. Instale o APK. Abra o app e siga o passo a passo.
2. Android 13+ (APK fora da Play Store): *Informações do app > ⋮ > Permitir configurações restritas*, depois ative em *Acessibilidade*.

## Se a corrida não for lida
Em **Ajustes > Modo diagnóstico**, abra o app de corrida, espere uma oferta e toque em **Copiar leitura**.
Com esse texto dá para ajustar `core/OfferParser.kt` (as expressões regulares) e os testes em `app/src/test`.

## Pontos de ajuste
- Nome: `res/values/strings.xml`. Pacote: `applicationId` em `app/build.gradle.kts`.
- Apps lidos: `res/xml/accessibility_service_config.xml` e `OfferAccessibilityService.TARGETS`.
- Assinatura: o release usa a chave debug. Para publicar na Play Store, configure sua própria chave.
