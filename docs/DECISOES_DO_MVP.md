# Decisões do primeiro incremento

## Prioridade

O aplicativo passou a atender uma loja local de moda praia. O primeiro fluxo implementado é o controle de estoque; pedidos, clientes e finanças não são requisito de navegação deste incremento.

## Unidade de estoque

O saldo é controlado por variação, e não somente por modelo. Uma variação é composta pelo produto, tamanho e cor e possui SKU, quantidade atual e quantidade mínima.

## Regras aplicadas

- entradas aumentam o saldo;
- saídas por venda diminuem o saldo;
- a quantidade não pode ser zero ou negativa;
- uma saída que produziria saldo negativo é recusada;
- o alerta de estoque baixo é exibido quando a quantidade é menor ou igual ao mínimo da variação.

## Próximo encaixe técnico

`InventoryRepository` é o ponto de troca da fonte de dados. A implementação atual em memória valida a experiência e as regras locais. A implementação seguinte deve consumir uma API REST via HTTPS, que aplica as mesmas regras de saldo e persiste produtos, variações e movimentações no PostgreSQL.
