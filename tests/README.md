# Testes

Esta pasta reúne o plano de testes, evidências manuais e outros artefatos acadêmicos de validação.

Os testes automatizados Android devem seguir a estrutura padrão do módulo:

- `app/src/test/`: testes unitários locais;
- `app/src/androidTest/`: testes instrumentados e de interface.

O comando de validação da baseline é `./gradlew test lintDebug assembleDebug` (ou `.\gradlew.bat` no Windows).
