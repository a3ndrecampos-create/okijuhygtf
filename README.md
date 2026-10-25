# Arka - Cálculo de Ganhos

App Android (Kotlin + Jetpack Compose) que lê a oferta de corrida na tela do Uber, 99, inDrive, iFood e Keeta Entregador
e mostra um semáforo (verde / amarelo / vermelho) com R$/km, R$/hora e lucro real.

## Publicar na Play Store
Veja **PUBLICAR.md** (chave de envio, .aab, assinatura mensal, declarações) e `store/ficha-play-store.md` (textos e imagens da ficha).

## Gerar o APK no GitHub
1. Crie um repositório e envie todos os arquivos (inclusive a pasta `.github`).
2. Aba **Actions** > **Build APK e AAB** > **Run workflow** (também roda a cada push). Os testes rodam antes do build.
3. Baixe **Arka-APK** (`Arka-debug.apk` e `Arka-release.apk`) e **Arka-AAB** (`Arka-PLAYSTORE.aab`, só sai assinado depois de cadastrar a chave: veja PUBLICAR.md).
4. Para publicar uma versão: crie uma tag `v1.1.0` e o APK vai para a aba **Releases**.

## Instalar e ativar
1. Instale o APK. Abra o app e siga o passo a passo.
2. Android 13+ (APK fora da Play Store): *Informações do app > ⋮ > Permitir configurações restritas*, depois ative em *Acessibilidade*.

## Se a corrida não for lida
Ajuste as expressões regulares em `core/OfferParser.kt` e os testes em `app/src/test`.

## Pontos de ajuste
- Nome: `res/values/strings.xml`. Pacote: `applicationId` em `app/build.gradle.kts`.
- Apps lidos: `res/xml/accessibility_service_config.xml` e `OfferAccessibilityService.TARGETS`.
- Assinatura de publicação: chave de envio vinda dos segredos do GitHub (PUBLICAR.md). Sem ela o release sai com a chave debug e a Play recusa.
- Teste grátis e assinatura: `billing/BillingManager.kt` (produto `arka_mensal`) e `core/Trial.kt` (60 dias).
