# Publicar o Arka na Play Store

Pacote: `com.arka.ganhos` · Teste grátis: 60 dias (no app) · Depois: assinatura mensal `arka_mensal`

## 1. Gerar a chave de envio (uma vez só)
1. No GitHub: **Actions > Gerar Keystore (rodar uma vez só) > Run workflow**.
2. Ao terminar, baixe o arquivo **arka-keystore** (zip) na página da execução.
3. Abra `SEGREDOS.txt` e cadastre 4 segredos em **Settings > Secrets and variables > Actions**:
   `ARKA_KEYSTORE_BASE64`, `ARKA_KEYSTORE_PASSWORD`, `ARKA_KEY_ALIAS`, `ARKA_KEY_PASSWORD`.
4. Guarde `arka-upload.jks` e o `SEGREDOS.txt` em lugar seguro (nuvem pessoal/pen drive). Depois **não deixe** o artefato no GitHub (ele some sozinho em 2 dias).
   Com o Play App Signing (padrão) uma chave de envio perdida pode ser trocada pelo suporte, mas dá trabalho.

## 2. Gerar o .aab
1. **Actions > Build APK e AAB > Run workflow**.
2. Baixe o artefato **Arka-AAB**. O arquivo certo se chama **`Arka-PLAYSTORE.aab`**.
   Se vier `Arka-SEM-CHAVE-nao-enviar.aab`, os 4 segredos não foram cadastrados: a Play recusa esse arquivo.
3. A cada novo envio basta rodar de novo: o número da versão sobe sozinho.

## 3. Play Console
1. Criar app: nome `Arka - Cálculo de Ganhos`, português (Brasil), app, gratuito.
2. **Monetizar > Assinaturas**: crie `arka_mensal` com plano base `mensal` (1 mês, renovação automática) e ative.
   Não crie oferta de teste grátis: os 60 dias já são feitos no app.
3. **Política de privacidade**: ative o GitHub Pages (Settings > Pages > Branch main, pasta `/docs`), edite o e-mail em `docs/privacidade.html` e use o link `https://SEU-USUARIO.github.io/NOME-DO-REPO/privacidade.html` no Play Console. Cole o mesmo link em `PRIVACY_URL` (`SettingsScreen.kt`).
4. **Conteúdo do app**: declaração de Acessibilidade (textos e roteiro do vídeo em `store/ficha-play-store.md`), segurança dos dados, público 18+, classificação de conteúdo, anúncios: não.
5. **Teste interno ou fechado**: envie o `.aab`. Para testar a cobrança sem pagar, cadastre seu e-mail em Configurações > Teste de licença.
   Contas pessoais criadas depois de nov/2023 precisam de um teste fechado com 12 testadores por 14 dias antes de ir para produção.
6. Produção: depois de aprovado, promova a versão.

## Pontos de atenção
- **Acessibilidade:** a Play revisa o uso dessa permissão por app. Um app que lê a tela de outros apps pode ser pedido a explicar ou ser recusado. O aviso com aceite já está no app; falta só o vídeo e a declaração.
- **Teste de 60 dias:** é contado no aparelho. Quem desinstala e reinstala ganha mais 60 dias. Para travar isso é preciso um servidor ou uma oferta de teste na própria Play (que exige cadastrar o pagamento antes).
- Antes de enviar, instale o `Arka-release.apk` no celular e confira a leitura da Uber/99 e o aviso de Acessibilidade.
