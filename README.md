# simple-bank
Банковское приложение на микросервисной архитектуре.

### Запуск
Запуск и развертка производится с помощью Docker Compose

`docker-compose up --build -d`

Приложение доступно по адресу `localhost:8080`

### Сервисы и подпроекты
- [bank-accounts](bank-accounts) сервис управления аккаунтами
- [bank-blocker](bank-blocker) сервис блокировки подозрительных транзакций
- [bank-cash](bank-cash) сервис пополнения и снятия денег со счета с помощью "наличных"
- [bank-configuration](bank-configuration) сервис конфигураций // удален за ненадобностью в 2.0
- [bank-exchange](bank-exchange) сервис обмена валют
- [bank-exchange-generator](bank-exchange-generator) сервис генерации курса обмена валют
- [bank-front](bank-front) фронтенд-сервис для веба
- [bank-notifications](bank-notifications) сервис нотификаций 
- [bank-transfer](bank-transfer) сервис переводов между счетами
- [gateway-server](gateway-server) Geteway-сервис для запросов 
- [commons](commons) - Библиотека стандартных функций и объектов, используемых более чем одним сервисом 

Для безопасности приложения используется авторизация на протоколе OAuth2. 
Пользователи для входа находятся в бд для сервиса `bank-accounts`. Регистрация по адресу `localhost:8080/signup` 