# Ficha da Play Store - Arka

## Textos

**Nome do app (máx. 30):** Arka - Cálculo de Ganhos

**Descrição curta (máx. 80):**
Veja na hora se a corrida vale a pena: semáforo verde, amarelo ou vermelho.

**Descrição completa:**
Aceitar ou recusar? O Arka responde em 1 segundo.

Quando uma oferta de corrida aparece no Uber, na 99, no inDrive, no iFood Entregador ou no Keeta Rider, o Arka calcula o seu lucro real e mostra um semáforo por cima do app:
🟢 Verde: boa corrida, aceite.
🟡 Amarelo: avalie.
🔴 Vermelho: não compensa.

O QUE VOCÊ TEM
• Lucro real por corrida, descontando combustível, manutenção e desgaste do carro
• R$/km e R$/hora de cada oferta
• Faixas do semáforo separadas por aplicativo (Uber, 99, inDrive, iFood Entregador, Keeta Rider), ajustáveis com controle deslizante
• Escolha de quais aplicativos monitorar
• Histórico das ofertas analisadas, com compartilhamento e exportação
• Calculadora para testar uma corrida antes de aceitar
• Funciona também em ofertas de negociação da 99
• Sem anúncios

PRIVACIDADE
Tudo é calculado no seu aparelho. O Arka não envia suas corridas nem capturas de tela para nenhum servidor e não aceita ou recusa corridas por você.

COMO ATIVAR
O Arka usa o serviço de Acessibilidade do Android para ler o valor, o tempo e a distância da oferta na tela dos aplicativos de corrida. Você autoriza dentro do app, em um aviso claro, e pode desligar quando quiser.

TESTE GRÁTIS E ASSINATURA
60 dias grátis. Depois, assinatura mensal pela Google Play, que você cancela quando quiser.

Aviso: o Arka é independente e não tem vínculo com Uber, 99, inDrive, iFood, Keeta ou outras empresas citadas.

**Categoria:** Mapas e navegação (ou Finanças; escolha a que preferir)
**Contato (e-mail público):** o seu
**Política de privacidade:** URL da página `docs/privacidade.html` publicada (GitHub Pages)

## Imagens (pasta store/)
- `icone-512.png` - ícone 512x512
- `grafico-destaque-1024x500.png` - gráfico de recursos 1024x500
- Capturas de tela: tire do seu celular (mínimo 2, ideal 4 a 8): tela inicial, semáforo sobre a Uber, histórico, faixas do semáforo.
  Evite mostrar a marca/logotipo dos apps de corrida nas capturas; se aparecerem, é por mostrar o funcionamento.

## Assinatura (Monetizar com o Play > Produtos > Assinaturas)
- ID do produto: `arka_mensal` (precisa ser exatamente este)
- Nome: Arka mensal
- Plano base: ID `mensal`, cobrança a cada 1 mês, renovação automática
- Preço: o que você decidir
- Teste grátis de 60 dias: já é feito dentro do app (contado desde a instalação). Não crie oferta de teste na Play, senão o usuário ganha 60 dias a mais.

## Declaração da API de Acessibilidade (Conteúdo do app)
**Principal funcionalidade do app que usa a API:**
Leitura do valor, do tempo e da distância das ofertas de corrida exibidas nos apps de corrida (Uber, 99, inDrive, iFood Entregador, Keeta Rider) para calcular o lucro do motorista e mostrar um semáforo (verde, amarelo, vermelho). O app não é uma ferramenta de acessibilidade e não declara isAccessibilityTool.

**Por que a API é necessária:**
Os apps de corrida não oferecem outra forma de o motorista ver o cálculo de lucro no momento da oferta. A leitura da tela é a única forma de obter valor, tempo e distância da oferta enquanto ela está visível.

**Dados coletados ou compartilhados pela API:**
Nenhum dado sai do aparelho. O texto da oferta e as capturas de tela são processados localmente e descartados; só valor, km e minutos ficam no histórico do aparelho.

**Ações no lugar do usuário:** nenhuma. O app não toca, não aceita nem recusa corridas.

**Vídeo:** grave 30 a 60 segundos mostrando (1) o aviso "Permissão de Acessibilidade" com o botão "Concordo e continuar", (2) a ativação em Ajustes > Acessibilidade, (3) uma oferta aparecendo e o semáforo por cima. O aviso do app (não o texto da tela de Acessibilidade do Android) é o que precisa aparecer.

## Segurança dos dados (Data safety)
- Coleta ou compartilha dados do usuário? Não coleta e não compartilha (os dados ficam só no aparelho).
- Compras no app: Sim (assinatura).
- Dados criptografados em trânsito: não se aplica (nada é enviado).
- Pedido de exclusão: não se aplica (dados só no aparelho; o usuário apaga pelo app ou desinstalando).
- Anúncios: não.

## Outras declarações
- Público-alvo: 18+
- Classificação de conteúdo: questionário, sem conteúdo sensível
- App de notícias/saúde/finanças reguladas: não
- Permissões: `BIND_ACCESSIBILITY_SERVICE` (serviço) e `com.android.vending.BILLING` (da biblioteca de cobrança)
